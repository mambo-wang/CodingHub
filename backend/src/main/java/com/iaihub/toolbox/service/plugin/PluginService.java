package com.iaihub.toolbox.service.plugin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iaihub.toolbox.config.UploadConfig;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.plugin.PluginCreateDraftRequest;
import com.iaihub.toolbox.dto.plugin.PluginDetailDTO;
import com.iaihub.toolbox.dto.plugin.PluginSummaryDTO;
import com.iaihub.toolbox.exception.BusinessException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Plugin;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.tag.PluginTag;
import com.iaihub.toolbox.model.tag.Tag;
import com.iaihub.toolbox.repository.PluginRepository;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.tag.PluginTagRepository;
import com.iaihub.toolbox.repository.tag.TagRepository;
import com.iaihub.toolbox.dto.tag.TagDTO;
import com.iaihub.toolbox.service.tag.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RepositoryCache;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 插件市场核心服务：zip 上传解析（B 档校验 + 组件摘要）、CRUD、marketplace.json 聚合。
 *
 * <p>上传流程（ADR-0002 决策）：临时解压 → 定位并解析 .codebuddy-plugin/plugin.json →
 * 生成组件摘要 → 校验通过后持久化 zip 原件 → 清理临时目录。覆盖式更新：重传 zip，
 * version 未变化则拒绝。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PluginService {

    private static final String PLUGIN_JSON_PREFERRED = ".codebuddy-plugin/plugin.json";
    private static final String PLUGIN_JSON_ROOT = "plugin.json";
    private static final long MAX_UNZIP_BYTES = 200L * 1024 * 1024;
    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024;
    private static final java.util.regex.Pattern NAME_PATTERN =
            java.util.regex.Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final java.util.regex.Pattern SOURCE_PATTERN =
            java.util.regex.Pattern.compile("^(https?://[^\\s]+|[a-zA-Z0-9_-]+/[a-zA-Z0-9._-]+)$");
    /** source 选填时的默认占位值（内网自研插件，不使用外部 GitHub 源） */
    private static final String DEFAULT_SOURCE = "internal/local";

    private final PluginRepository pluginRepository;
    private final UserRepository userRepository;
    private final UploadConfig uploadConfig;
    private final ObjectMapper objectMapper;
    private final PluginTagRepository pluginTagRepository;
    private final TagRepository tagRepository;
    private final TagService tagService;

    // ---------- 上传 ----------

    @Transactional
    public PluginDetailDTO upload(MultipartFile file, String source, String logoUrl, Long userId, List<Long> tagIds) {
        validateFile(file);
        String src = normalizeSource(source);

        Path base = basePluginsDir();
        Path tmpDir = base.resolve("tmp").resolve(UUID.randomUUID().toString());
        try {
            Files.createDirectories(tmpDir);

            // 临时解压
            extractZip(file.getInputStream(), tmpDir);

            // 定位并解析 plugin.json
            Path pluginJsonPath = findPluginJson(tmpDir)
                    .orElseThrow(() -> new BusinessException(400, "压缩包中未找到 plugin.json（应在 .codebuddy-plugin/ 目录或根目录）"));
            Map<String, Object> meta = parseJson(pluginJsonPath);

            String name = requiredString(meta, "name");
            String version = requiredString(meta, "version");
            validateName(name);
            if (pluginRepository.existsByName(name)) {
                throw new BusinessException(400, "插件名已存在: " + name + "（如需更新请走覆盖式更新接口）");
            }

            String description = str(meta.get("description"));
            if (description == null || description.isBlank()) {
                description = "暂无描述";
            }
            if (description.length() > 5000) {
                description = description.substring(0, 5000);
            }

            User author = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("用户不存在", userId));

            // 组件摘要
            String componentsJson = inspectComponents(tmpDir);

            Plugin plugin = Plugin.builder()
                    .name(name)
                    .description(description)
                    .version(version)
                    .author(author)
                    .logoUrl(resolveLogoUrl(meta, logoUrl))
                    .source(src)
                    .pluginJson(objectMapper.writeValueAsString(meta))
                    .components(componentsJson)
                    .build();
            plugin = pluginRepository.save(plugin);

            // 持久化 zip 原件
            Path dest = persistZip(file, plugin.getId());
            plugin.setSourceZipPath(relativizeBase(dest));
            pluginRepository.save(plugin);

            // 标签关联（上传时同事务写入）
            if (tagIds != null && !tagIds.isEmpty()) {
                replaceTags(plugin.getId(), tagIds);
            }

            // 生成 bare git 仓库，供 CodeBuddy URL 市场 git clone 安装
            try {
                initGitRepo(tmpDir, name, version);
            } catch (Exception e) {
                log.warn("生成 git 仓库失败（不影响上传，仅影响 URL 市场安装）: name={}", name, e);
            }

            return toDetailDTO(plugin);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("插件上传解析失败", e);
            throw new BusinessException(400, "插件上传失败: " + e.getMessage());
        } finally {
            deleteQuietly(tmpDir);
        }
    }

    // ---------- MCP 两段式创建：草稿 + 补全 ----------

    /**
     * 第一步：仅保存插件元数据，status=DRAFT（不进入市场、不参与列表）。
     * 供 MCP 智能体通过 createDraft 建立插件，随后上传 zip 补全。
     */
    @Transactional
    public PluginDetailDTO createDraft(PluginCreateDraftRequest req, Long userId) {
        String name = req.getName().trim();
        String source = normalizeSource(req.getSource());
        validateName(name);
        if (pluginRepository.existsByName(name)) {
            throw new BusinessException(400, "插件名已存在: " + name);
        }
        String version = req.getVersion().trim();

        String description = req.getDescription() == null || req.getDescription().isBlank()
                ? "暂无描述" : req.getDescription().trim();
        if (description.length() > 5000) {
            description = description.substring(0, 5000);
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在", userId));

        Plugin plugin = Plugin.builder()
                .name(name)
                .description(description)
                .version(version)
                .author(author)
                .source(source)
                .status(Plugin.Status.DRAFT)
                .build();
        plugin = pluginRepository.save(plugin);
        return toDetailDTO(plugin);
    }

    /**
     * 第二步：为草稿插件补全 zip 文件。解压校验 plugin.json、生成组件摘要、
     * 持久化 zip 原件、生成 bare git 仓库，并将 status 从 DRAFT 置为 NORMAL。
     */
    @Transactional
    public PluginDetailDTO finalizeUpload(Long id, MultipartFile file) {
        Plugin plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("插件不存在", id));
        if (plugin.getStatus() != Plugin.Status.DRAFT) {
            throw new BusinessException(400, "仅草稿状态插件可补全 zip（当前状态: " + plugin.getStatus() + "）");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传插件 zip 包");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(400, "zip 文件大小不能超过 50MB");
        }

        Path base = basePluginsDir();
        Path tmpDir = base.resolve("tmp").resolve(UUID.randomUUID().toString());
        try {
            Files.createDirectories(tmpDir);
            extractZip(file.getInputStream(), tmpDir);

            Path pluginJsonPath = findPluginJson(tmpDir)
                    .orElseThrow(() -> new BusinessException(400, "压缩包中未找到 plugin.json（应在 .codebuddy-plugin/ 目录或根目录）"));
            Map<String, Object> meta = parseJson(pluginJsonPath);

            String zipName = requiredString(meta, "name");
            String zipVersion = requiredString(meta, "version");
            if (!plugin.getName().equals(zipName)) {
                throw new BusinessException(400, "zip 包中的 name 必须与草稿一致");
            }
            if (!plugin.getVersion().equals(zipVersion)) {
                throw new BusinessException(400, "zip 包中的 version 必须与草稿一致");
            }

            String description = str(meta.get("description"));
            if (description == null || description.isBlank()) {
                description = "暂无描述";
            }
            if (description.length() > 5000) {
                description = description.substring(0, 5000);
            }

            plugin.setDescription(description);
            plugin.setLogoUrl(str(meta.get("icon")));
            plugin.setPluginJson(objectMapper.writeValueAsString(meta));
            plugin.setComponents(inspectComponents(tmpDir));

            // 持久化 zip 原件
            Path dest = persistZip(file, plugin.getId());
            plugin.setSourceZipPath(relativizeBase(dest));

            // 转为 NORMAL，进入市场
            plugin.setStatus(Plugin.Status.NORMAL);
            pluginRepository.save(plugin);

            // 生成 bare git 仓库
            try {
                initGitRepo(tmpDir, plugin.getName(), plugin.getVersion());
            } catch (Exception e) {
                log.warn("生成 git 仓库失败（不影响上传，仅影响 URL 市场安装）: name={}", plugin.getName(), e);
            }

            return toDetailDTO(plugin);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("插件草稿补全失败: id={}", id, e);
            throw new BusinessException(400, "插件补全失败: " + e.getMessage());
        } finally {
            deleteQuietly(tmpDir);
        }
    }

    // ---------- 覆盖式更新 ----------

    @Transactional
    public PluginDetailDTO update(Long id, MultipartFile file, String source, String logoUrl, Long userId, boolean isAdmin, List<Long> tagIds) {
        Plugin plugin = getNormal(id);
        checkPermission(plugin, userId, isAdmin);

        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传插件 zip 包");
        }
        String src = normalizeSource(source);
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(400, "zip 文件大小不能超过 50MB");
        }

        Path base = basePluginsDir();
        Path tmpDir = base.resolve("tmp").resolve(UUID.randomUUID().toString());
        try {
            Files.createDirectories(tmpDir);
            extractZip(file.getInputStream(), tmpDir);

            Path pluginJsonPath = findPluginJson(tmpDir)
                    .orElseThrow(() -> new BusinessException(400, "压缩包中未找到 plugin.json"));
            Map<String, Object> meta = parseJson(pluginJsonPath);

            String newName = requiredString(meta, "name");
            String newVersion = requiredString(meta, "version");
            if (!plugin.getName().equals(newName)) {
                throw new BusinessException(400, "更新包的 name 必须与现有插件一致");
            }
            if (plugin.getVersion().equals(newVersion)) {
                throw new BusinessException(400, "版本未变化，拒绝覆盖更新");
            }

            String description = str(meta.get("description"));
            if (description == null || description.isBlank()) {
                description = "暂无描述";
            }
            if (description.length() > 5000) {
                description = description.substring(0, 5000);
            }

            plugin.setVersion(newVersion);
            plugin.setDescription(description);
            plugin.setLogoUrl(resolveLogoUrl(meta, logoUrl));
            plugin.setSource(src);
            plugin.setPluginJson(objectMapper.writeValueAsString(meta));
            plugin.setComponents(inspectComponents(tmpDir));

            // 覆盖 zip 原件
            Path dest = persistZip(file, plugin.getId());
            plugin.setSourceZipPath(relativizeBase(dest));
            pluginRepository.save(plugin);

            // 标签替换（请求带 tagIds 字段才替换，对齐 ToolService 语义）
            if (tagIds != null) {
                replaceTags(plugin.getId(), tagIds);
            }

            // 重新生成 bare git 仓库
            try {
                initGitRepo(tmpDir, plugin.getName(), newVersion);
            } catch (Exception e) {
                log.warn("重新生成 git 仓库失败: name={}", plugin.getName(), e);
            }

            return toDetailDTO(plugin);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("插件覆盖更新失败: id={}", id, e);
            throw new BusinessException(400, "插件更新失败: " + e.getMessage());
        } finally {
            deleteQuietly(tmpDir);
        }
    }

    // ---------- 标签 ----------

    /** 重建插件标签关联（先减旧标签 usage，再挂新标签并加 usage）。传 null 视为清空。 */
    @Transactional
    public void replaceTags(Long pluginId, List<Long> tagIds) {
        List<PluginTag> oldTags = pluginTagRepository.findByPluginId(pluginId);
        for (PluginTag pt : oldTags) {
            tagRepository.findById(pt.getTagId()).ifPresent(Tag::decrementUsage);
        }
        pluginTagRepository.deleteByPluginId(pluginId);
        pluginTagRepository.flush();
        if (tagIds == null) return;
        for (Long tagId : tagIds) {
            pluginTagRepository.save(new PluginTag(pluginId, tagId));
            tagRepository.findById(tagId).ifPresent(Tag::incrementUsage);
        }
    }

    /** 加载插件关联的标签 DTO 列表。 */
    private List<TagDTO> loadTags(Long pluginId) {
        return pluginTagRepository.findByPluginId(pluginId).stream()
                .map(pt -> tagRepository.findById(pt.getTagId()).orElse(null))
                .filter(Objects::nonNull)
                .map(tagService::toDTO)
                .toList();
    }

    // ---------- 查询 ----------

    @Transactional(readOnly = true)
    public PageResponse<PluginSummaryDTO> list(String keyword, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Plugin> pluginPage;
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        if ("hot".equalsIgnoreCase(sort)) {
            pluginPage = pluginRepository.findByFiltersOrderByHot(kw, pageable);
        } else {
            pluginPage = pluginRepository.findByFilters(kw, pageable);
        }

        List<PluginSummaryDTO> items = pluginPage.getContent().stream()
                .map(this::toSummaryDTO)
                .toList();

        return PageResponse.<PluginSummaryDTO>builder()
                .content(items)
                .totalElements(pluginPage.getTotalElements())
                .totalPages(pluginPage.getTotalPages())
                .page(page)
                .size(size)
                .build();
    }

    public void pinPlugin(Long id) {
        pluginRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("插件不存在"));
        pluginRepository.pinById(id);
    }

    public void unpinPlugin(Long id) {
        pluginRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("插件不存在"));
        pluginRepository.unpinById(id);
    }

    public List<Long> getHotTop5() {
        return pluginRepository.findTop5ByStatusOrderByScoreDesc(PageRequest.of(0, 5));
    }

    @Transactional
    public PluginDetailDTO getDetail(Long id) {
        // 浏览量原子 +1（不触发 @PreUpdate，避免 updatedAt 被刷新为当前时间）
        pluginRepository.incrementViewCount(id);

        Plugin plugin = getNormal(id);
        return toDetailDTO(plugin);
    }

    // ---------- 删除 ----------

    @Transactional
    public void delete(Long id, Long userId, boolean isAdmin) {
        Plugin plugin = getNormal(id);
        checkPermission(plugin, userId, isAdmin);
        plugin.setStatus(Plugin.Status.DELETED);
        pluginRepository.save(plugin);
    }

    // ---------- marketplace.json ----------

    @Transactional(readOnly = true)
    public Map<String, Object> getMarketplace() {
        List<Plugin> plugins = pluginRepository.findAllByStatusOrderByCreatedAtDesc(Plugin.Status.NORMAL);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Plugin plugin : plugins) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", plugin.getName());
            // source 用 git 对象，指向内置 git 服务器，CodeBuddy 通过 git clone 安装。
            // 仓库目录名带版本号（<name>-<version>.git，见 initGitRepo），更新版本后 URL 随之变化，
            // 客户端刷新市场时按新 URL 拉取最新内容
            String gitUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/git/{name}-{version}.git")
                    .buildAndExpand(plugin.getName(), plugin.getVersion())
                    .toUriString();
            Map<String, Object> sourceObj = new LinkedHashMap<>();
            sourceObj.put("source", "url");
            sourceObj.put("url", gitUrl);
            item.put("source", sourceObj);
            item.put("description", plugin.getDescription());
            item.put("version", plugin.getVersion());
            item.put("author", plugin.getAuthor() != null ? plugin.getAuthor().getUsername() : "");
            items.add(item);
        }

        Map<String, Object> owner = new LinkedHashMap<>();
        owner.put("name", "CodingHub");
        owner.put("email", "");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", "codinghub-market");
        result.put("owner", owner);
        result.put("description", "CodingHub 插件市场（由插件表实时聚合生成）");
        result.put("version", "1.0.0");
        result.put("plugins", items);
        return result;
    }

    // ---------- zip 下载 ----------

    @Transactional(readOnly = true)
    public void downloadZip(Long id, OutputStream out) throws IOException {
        Plugin plugin = getNormal(id);
        if (plugin.getSourceZipPath() == null || plugin.getSourceZipPath().isBlank()) {
            throw new ResourceNotFoundException("插件 zip 原件不存在");
        }
        Path zipPath = Paths.get(uploadConfig.getBaseDir(), plugin.getSourceZipPath());
        if (!Files.exists(zipPath)) {
            throw new ResourceNotFoundException("插件 zip 原件不存在");
        }
        Files.copy(zipPath, out);
    }

    // ---------- marketplace.zip（zip 市场：插件源码随市场打包，source 用相对路径） ----------

    @Transactional(readOnly = true)
    public void downloadMarketplaceZip(OutputStream out) throws IOException {
        List<Plugin> plugins = pluginRepository.findAllByStatusOrderByCreatedAtDesc(Plugin.Status.NORMAL);

        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            // 1. 写入 marketplace.json，插件 source 用相对路径 ./<name>
            Map<String, Object> market = new LinkedHashMap<>();
            market.put("name", "codinghub-market");
            Map<String, Object> owner = new LinkedHashMap<>();
            owner.put("name", "CodingHub");
            owner.put("email", "");
            market.put("owner", owner);
            market.put("description", "CodingHub 插件市场（zip 分发，插件源码随市场打包）");
            market.put("version", "1.0.0");

            List<Map<String, Object>> items = new ArrayList<>();
            for (Plugin plugin : plugins) {
                if (plugin.getSourceZipPath() == null || plugin.getSourceZipPath().isBlank()) continue;
                if (!Files.exists(Paths.get(uploadConfig.getBaseDir(), plugin.getSourceZipPath()))) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", plugin.getName());
                item.put("source", "./" + plugin.getName());
                item.put("description", plugin.getDescription());
                item.put("version", plugin.getVersion());
                item.put("author", plugin.getAuthor() != null ? plugin.getAuthor().getUsername() : "");
                items.add(item);
            }
            market.put("plugins", items);

            zos.putNextEntry(new ZipEntry("marketplace.json"));
            zos.write(objectMapper.writeValueAsBytes(market));
            zos.closeEntry();

            // 2. 各插件源码目录：entry 统一带 <name>/ 前缀，并把 \ 归一为 /
            for (Plugin plugin : plugins) {
                if (plugin.getSourceZipPath() == null || plugin.getSourceZipPath().isBlank()) continue;
                Path zipPath = Paths.get(uploadConfig.getBaseDir(), plugin.getSourceZipPath());
                if (!Files.exists(zipPath)) continue;
                String prefix = plugin.getName() + "/";
                try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        zos.putNextEntry(new ZipEntry(prefix + entry.getName().replace('\\', '/')));
                        if (!entry.isDirectory()) {
                            zis.transferTo(zos);
                        }
                        zos.closeEntry();
                        zis.closeEntry();
                    }
                }
            }
        }
    }

    // ---------- 私有工具 ----------

    private Plugin getNormal(Long id) {
        return pluginRepository.findByIdAndStatusNormal(id)
                .orElseThrow(() -> new ResourceNotFoundException("插件不存在或已删除", id));
    }

    private void checkPermission(Plugin plugin, Long userId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (plugin.getAuthor() != null && plugin.getAuthor().getId().equals(userId)) {
            return;
        }
        throw new BusinessException(403, "无权操作此插件");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请上传插件 zip 包");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".zip")) {
            throw new BusinessException(400, "仅支持 .zip 格式的插件包");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BusinessException(400, "zip 文件大小不能超过 50MB");
        }
    }

    /**
     * source 为选填：未填写时使用默认占位值（内网自研插件场景），填写时校验格式。
     */
    private String normalizeSource(String source) {
        if (source == null || source.isBlank()) {
            return DEFAULT_SOURCE;
        }
        String s = source.trim();
        if (!SOURCE_PATTERN.matcher(s).matches()) {
            throw new BusinessException(400, "source 格式不合法：应为 GitHub owner/repo（如 owner/repo）或绝对 URL");
        }
        return s;
    }

    /**
     * 图标优先级：显式传入 logoUrl（含空串=清空）优先；未传入时回退到 plugin.json 的 icon 字段。
     */
    private String resolveLogoUrl(Map<String, Object> meta, String logoUrl) {
        if (logoUrl != null) {
            return logoUrl.isBlank() ? null : logoUrl.trim();
        }
        return str(meta.get("icon"));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "plugin.json 中 name 不能为空");
        }
        if (name.length() > 100) {
            throw new BusinessException(400, "插件名过长（<=100）");
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new BusinessException(400, "插件名必须为 kebab-case（小写字母/数字/连字符）: " + name);
        }
    }

    private String requiredString(Map<String, Object> meta, String key) {
        String value = str(meta.get(key));
        if (value == null || value.isBlank()) {
            throw new BusinessException(400, "plugin.json 缺少必填字段: " + key);
        }
        return value.trim();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Map<String, Object> parseJson(Path path) throws IOException {
        return objectMapper.readValue(Files.readString(path), new TypeReference<Map<String, Object>>() {});
    }

    private void extractZip(InputStream in, Path targetDir) throws IOException {
        long totalBytes = 0;
        try (ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = targetDir.resolve(entry.getName()).normalize();
                if (!outPath.startsWith(targetDir)) {
                    throw new BusinessException(400, "压缩包包含非法路径: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(outPath);
                } else {
                    Files.createDirectories(outPath.getParent());
                    totalBytes += copyWithLimit(zis, outPath, MAX_UNZIP_BYTES - totalBytes);
                }
            }
        }
    }

    private long copyWithLimit(InputStream in, Path out, long remaining) throws IOException {
        if (remaining <= 0) {
            throw new BusinessException(400, "解压后体积超过 200MB 限制");
        }
        long written = 0;
        byte[] buffer = new byte[8192];
        try (OutputStream os = Files.newOutputStream(out)) {
            int n;
            while ((n = in.read(buffer)) != -1) {
                written += n;
                if (written > remaining) {
                    throw new BusinessException(400, "解压后体积超过 200MB 限制");
                }
                os.write(buffer, 0, n);
            }
        }
        return written;
    }

    private Optional<Path> findPluginJson(Path root) throws IOException {
        Path preferred = root.resolve(PLUGIN_JSON_PREFERRED);
        if (Files.isRegularFile(preferred)) {
            return Optional.of(preferred);
        }
        Path fallback = root.resolve(PLUGIN_JSON_ROOT);
        if (Files.isRegularFile(fallback)) {
            return Optional.of(fallback);
        }
        return Optional.empty();
    }

    /** 枚举插件组件目录/文件，生成组件摘要 JSON 字符串。 */
    private String inspectComponents(Path root) throws IOException {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("skills", listDirs(root.resolve("skills")));
        summary.put("agents", listDirs(root.resolve("agents")));
        summary.put("commands", listDirs(root.resolve("commands")));
        summary.put("hooks", listDirs(root.resolve("hooks")));
        summary.put("mcpServers", Files.isRegularFile(root.resolve(".mcp.json")));
        summary.put("lspServers", Files.isRegularFile(root.resolve(".lsp.json")));
        summary.put("hasBin", Files.isDirectory(root.resolve("bin")));
        summary.put("hasSettings", Files.isRegularFile(root.resolve("settings.json")));
        return objectMapper.writeValueAsString(summary);
    }

    private List<String> listDirs(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    names.add(p.getFileName().toString());
                }
            }
        } catch (IOException e) {
            log.warn("枚举插件组件目录失败: {}", dir, e);
        }
        return names;
    }

    private Path persistZip(MultipartFile file, Long pluginId) throws IOException {
        Path pluginDir = basePluginsDir().resolve(String.valueOf(pluginId));
        Files.createDirectories(pluginDir);
        Path dest = pluginDir.resolve("source.zip");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return dest;
    }

    /**
     * 将解压后的插件目录初始化为 bare git 仓库，供 CodeBuddy URL 市场 {@code git clone} 安装。
     *
     * <p>仓库位于 {@code <base-dir>/git-repos/<name>-<version>.git}（与 {@link GitHttpConfig} 的根目录一致）。
     * 目录名携带版本号：更新版本时生成新路径的仓库，无需删除旧仓库，避免 Windows 上被
     * git 服务（JGit RepositoryCache）缓存锁定的旧仓库无法删除、重建失败的问题。
     * {@link #getMarketplace()} 中的 {@code source.url} 同步指向带版本的仓库路径。</p>
     */
    private void initGitRepo(Path tmpDir, String name, String version) throws IOException, GitAPIException {
        Path reposRoot = Paths.get(uploadConfig.getBaseDir(), "git-repos");
        Files.createDirectories(reposRoot);
        Path bareRepo = reposRoot.resolve(name + "-" + version + ".git");
        // 同版本重传（非常规路径）时清理旧目录；不同版本之间互不冲突
        if (Files.exists(bareRepo)) {
            RepositoryCache.clear();
            deleteQuietly(bareRepo);
        }

        // 临时工作目录：tmpDir 已解压插件内容，直接作为 working tree 提交
        try (Git git = Git.init().setDirectory(tmpDir.toFile()).call()) {
            git.add().addFilepattern(".").call();
            git.commit()
                    .setMessage("plugin " + name + " v" + version)
                    .setAuthor("CodingHub", "codinghub@local")
                    .setCommitter("CodingHub", "codinghub@local")
                    .call();
        }

        // 从 working tree 克隆为 bare 仓库
        try (Git ignored = Git.cloneRepository()
                .setURI(tmpDir.toUri().toString())
                .setBare(true)
                .setDirectory(bareRepo.toFile())
                .call()) {
            // 克隆完成即生成 bare 仓库
        }
    }

    private String relativizeBase(Path path) {
        Path base = Paths.get(uploadConfig.getBaseDir()).toAbsolutePath().normalize();
        // base 已是 upload 根目录，relativize 结果已含 "plugins/" 前缀，不能再重复拼接
        return base.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private Path basePluginsDir() {
        Path base = Paths.get(uploadConfig.getBaseDir());
        Path dir = base.resolve("plugins");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new BusinessException(500, "无法创建插件存储目录: " + dir);
        }
        return dir;
    }

    private void deleteQuietly(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try {
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            log.warn("清理临时目录失败: {}", p, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}", dir, e);
        }
    }

    private PluginSummaryDTO toSummaryDTO(Plugin plugin) {
        User author = plugin.getAuthor();
        return PluginSummaryDTO.builder()
                .id(plugin.getId())
                .name(plugin.getName())
                .description(plugin.getDescription())
                .version(plugin.getVersion())
                .logoUrl(plugin.getLogoUrl())
                .source(plugin.getSource())
                .likeCount(plugin.getLikeCount())
                .commentCount(plugin.getCommentCount())
                .viewCount(plugin.getViewCount())
                .favoriteCount(plugin.getFavoriteCount())
                .pinned(plugin.getPinned())
                .tags(loadTags(plugin.getId()))
                .score(plugin.getScore())
                .authorId(author != null ? author.getId() : null)
                .authorUsername(author != null ? author.getUsername() : "Unknown")
                .authorNickname(author != null ? author.getNickname() : null)
                .createdAt(plugin.getCreatedAt())
                .build();
    }

    private PluginDetailDTO toDetailDTO(Plugin plugin) {
        PluginSummaryDTO summary = toSummaryDTO(plugin);
        PluginDetailDTO dto = new PluginDetailDTO();
        copySummary(summary, dto);
        dto.setComponents(parseComponents(plugin.getComponents()));
        dto.setPluginJson(parsePluginJson(plugin.getPluginJson()));
        if (plugin.getSourceZipPath() != null) {
            String zipName = plugin.getSourceZipPath().contains("/")
                    ? plugin.getSourceZipPath().substring(plugin.getSourceZipPath().lastIndexOf('/') + 1)
                    : plugin.getSourceZipPath();
            dto.setSourceZipName(plugin.getName() + "-" + plugin.getVersion() + ".zip");
            if (zipName.endsWith(".zip") && !"source.zip".equals(zipName)) {
                dto.setSourceZipName(zipName);
            }
        }
        return dto;
    }

    private void copySummary(PluginSummaryDTO from, PluginSummaryDTO to) {
        to.setId(from.getId());
        to.setName(from.getName());
        to.setDescription(from.getDescription());
        to.setVersion(from.getVersion());
        to.setLogoUrl(from.getLogoUrl());
        to.setSource(from.getSource());
        to.setLikeCount(from.getLikeCount());
        to.setCommentCount(from.getCommentCount());
        to.setViewCount(from.getViewCount());
        to.setFavoriteCount(from.getFavoriteCount());
        to.setPinned(from.getPinned());
        to.setTags(from.getTags());
        to.setScore(from.getScore());
        to.setAuthorId(from.getAuthorId());
        to.setAuthorUsername(from.getAuthorUsername());
        to.setAuthorNickname(from.getAuthorNickname());
        to.setCreatedAt(from.getCreatedAt());
    }

    private List<PluginDetailDTO.ComponentSummary> parseComponents(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            return List.of(objectMapper.convertValue(map, PluginDetailDTO.ComponentSummary.class));
        } catch (Exception e) {
            log.warn("解析组件摘要失败", e);
            return List.of();
        }
    }

    private Map<String, Object> parsePluginJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析 plugin.json 失败", e);
            return Map.of();
        }
    }
}
