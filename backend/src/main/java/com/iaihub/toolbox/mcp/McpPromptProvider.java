package com.iaihub.toolbox.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Prompt 模板提供者 — 将 QuickStart 页面的工作流提示词封装为标准 MCP Prompt
 *
 * <p>注册 8 个 prompt 模板，用户无需安装 CodingHub Skill 即可通过 MCP 客户端
 * （CodeBuddy、QoderWork 等）直接使用这些预置工作流：
 *
 * <ol>
 *   <li>{@code search-tools} — 搜索工具广场</li>
 *   <li>{@code install-tool} — 安装工具到本地项目</li>
 *   <li>{@code check-versions} — 检查工具版本更新</li>
 *   <li>{@code publish-tool} — 发布本地 Skill 到工具广场</li>
 *   <li>{@code update-tool} — 更新已发布的工具</li>
 *   <li>{@code publish-plugin} — 制作并发布符合规范的插件包到插件市场</li>
 *   <li>{@code update-plugin} — 更新已发布的插件</li>
 *   <li>{@code forum-post} — 发帖到论坛</li>
 * </ol>
 */
@Component
public class McpPromptProvider {

    private static final Logger logger = LoggerFactory.getLogger(McpPromptProvider.class);

    /**
     * 构建所有 prompt 规格列表，供 McpSdkServerConfig 注册。
     */
    public List<McpServerFeatures.SyncPromptSpecification> buildAll() {
        return List.of(
                searchTools(),
                installTool(),
                checkVersions(),
                publishTool(),
                updateTool(),
                publishPlugin(),
                updatePlugin(),
                forumPost()
        );
    }

    // ── 1. 搜索工具 ───────────────────────────────────────────────

    private McpServerFeatures.SyncPromptSpecification searchTools() {
        McpSchema.Prompt prompt = McpSchema.Prompt.builder("search-tools")
                .title("搜索工具")
                .description("在 CodingHub 工具广场搜索可用工具，可按关键词和分类筛选")
                .arguments(List.of(
                        McpSchema.PromptArgument.builder("query")
                                .description("搜索关键词，留空则返回全部")
                                .required(false)
                                .build()
                ))
                .build();

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, req) -> {
            Map<String, Object> args = req.arguments() != null ? req.arguments() : Map.of();
            String query = str(args, "query");
            String queryDesc = query.isEmpty() ? "全部工具" : "关键词「" + query + "」";

            String text = """
                    请帮我搜索 CodingHub 工具广场中的工具。搜索条件: %s。
                    
                    操作步骤:
                    1. 调用 h3_coding_hub_tool_search 工具，传入 query="%s"（留空则不传）
                    2. 将返回的工具列表以表格形式展示：工具名称、分类、版本、简介
                    3. 如果结果较多，按分类分组展示
                    4. 提示用户可以进一步搜索特定工具的详情或安装
                    """.formatted(queryDesc, query);

            return userPromptResult("搜索 CodingHub 工具广场", text);
        });
    }

    // ── 2. 安装工具 ───────────────────────────────────────────────

    private McpServerFeatures.SyncPromptSpecification installTool() {
        McpSchema.Prompt prompt = McpSchema.Prompt.builder("install-tool")
                .title("安装工具")
                .description("从 CodingHub 工具广场获取 Skill 的完整信息和文件，安装到当前项目")
                .arguments(List.of(
                        McpSchema.PromptArgument.builder("toolName")
                                .description("要安装的工具/Skill 名称")
                                .required(true)
                                .build()
                ))
                .build();

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, req) -> {
            Map<String, Object> args = req.arguments() != null ? req.arguments() : Map.of();
            String toolName = str(args, "toolName");

            String text = """
                    请帮我把 CodingHub 工具广场上的「%s」安装到当前项目。
                    
                    操作步骤:
                    1. 调用 h3_coding_hub_tool_search 搜索「%s」，找到对应的 toolId
                    2. 调用 h3_coding_hub_tool_get 获取工具的完整文档（含安装说明）
                    3. 调用 h3_coding_hub_tool_files 获取该工具的文件列表
                    4. 对每个文件，调用 h3_coding_hub_tool_download 获取下载链接
                       - 注意: 返回的是相对路径，需拼接 http://<host>:8082 构成完整 URL
                    5. 用 curl 或 HTTP 客户端下载文件到本地 skill 目录
                       - 如果是 zip 包，解压到 skill 目录并保留目录结构
                       - 如果是单文件（如 SKILL.md），直接放到 skill 目录
                    6. 将工具版本号写到 skill 文件夹的 tools.version 文件中
                    7. 如果本地已有该 skill，则覆盖安装
                    8. 安装完成后展示安装结果和工具简介
                    """.formatted(toolName, toolName);

            return userPromptResult("安装 " + toolName + " 到当前项目", text);
        });
    }

    // ── 3. 版本检查 ──────────────────────────────────────────────

    private McpServerFeatures.SyncPromptSpecification checkVersions() {
        McpSchema.Prompt prompt = McpSchema.Prompt.builder("check-versions")
                .title("版本检查")
                .description("根据本地 skill 的 tools.version 版本号排查有无需要升级的工具")
                .arguments(List.of())
                .build();

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, req) -> {
            String text = """
                    请帮我检查当前项目中已安装的 CodingHub 工具是否有版本更新。
                    
                    操作步骤:
                    1. 扫描当前项目的 skill 目录，找到所有包含 tools.version 文件的 skill
                    2. 读取每个 tools.version 中的本地版本号
                    3. 调用 h3_coding_hub_tool_search 搜索对应工具名称
                    4. 对比远程版本号与本地版本号
                    5. 以表格形式展示检查结果：
                       - Skill 名称
                       - 本地版本
                       - 远程版本
                       - 状态（已是最新 / 可升级）
                    6. 如果有可升级的工具，提示用户是否要执行升级（可复用 install-tool prompt）
                    """;

            return userPromptResult("检查 CodingHub 工具版本更新", text);
        });
    }

    // ── 4. 发布工具 ───────────────────────────────────────────────

    private McpServerFeatures.SyncPromptSpecification publishTool() {
        McpSchema.Prompt prompt = McpSchema.Prompt.builder("publish-tool")
                .title("发布工具")
                .description("将本地 Skill 发布到 CodingHub 工具广场")
                .arguments(List.of(
                        McpSchema.PromptArgument.builder("skillName")
                                .description("要发布的本地 Skill 名称")
                                .required(true)
                                .build()
                ))
                .build();

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, req) -> {
            Map<String, Object> args = req.arguments() != null ? req.arguments() : Map.of();
            String skillName = str(args, "skillName");

            String text = """
                    请帮我把本地的「%s」Skill 发布到 CodingHub 工具广场。
                    
                    操作步骤:
                    1. 读取本地 %s skill 目录下的 SKILL.md，了解工具功能和结构
                    2. 准备工具描述（content），应包含：工具介绍、安装方法、使用示例
                    3. 确认 categoryId（可通过 h3_coding_hub_tool_search 查看现有工具的分类推断）
                    4. 调用 h3_coding_hub_tool_create 创建工具:
                       - name: %s
                       - content: 准备好的 markdown 描述
                       - version: 从 SKILL.md 的 frontmatter 读取，或默认 1.0.0
                       - username/password: 从记忆中获取，或询问用户
                    5. 记录返回的 toolId
                    6. 准备上传文件（重要）:
                       - 检查 skill 目录的文件数量
                       - 如果包含多个文件（不止 SKILL.md，如还有 references/、scripts/ 等），必须先将其压缩为 zip 包（保留目录结构）再上传
                       - 只有目录中仅有一个 SKILL.md 文件时，才可以直接上传该文件
                       - 严禁将多个文件逐个直接上传
                    7. 调用 h3_coding_hub_tool_file_upload 获取 REST 上传端点
                    8. 用 curl 执行 HTTP multipart POST 上传文件:
                       curl -X POST http://<host>:8082/api/v1/tools/{toolId}/files \\
                         -F "files=@/path/to/file.zip"
                    9. 上传完成后调用 h3_coding_hub_tool_get 确认发布成功
                    10. 将版本号写入本地 skill 目录的 tools.version 文件
                    """.formatted(skillName, skillName, skillName);

            return userPromptResult("发布 " + skillName + " 到 CodingHub", text);
        });
    }

    // ── 5. 更新工具 ───────────────────────────────────────────────

    private McpServerFeatures.SyncPromptSpecification updateTool() {
        McpSchema.Prompt prompt = McpSchema.Prompt.builder("update-tool")
                .title("更新工具")
                .description("将本地 Skill 的新版本更新到 CodingHub 工具广场的已有工具")
                .arguments(List.of(
                        McpSchema.PromptArgument.builder("skillName")
                                .description("要更新的 Skill 名称")
                                .required(true)
                                .build(),
                        McpSchema.PromptArgument.builder("version")
                                .description("新版本号（如 2.0.0），留空则自动递增")
                                .required(false)
                                .build()
                ))
                .build();

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, req) -> {
            Map<String, Object> args = req.arguments() != null ? req.arguments() : Map.of();
            String skillName = str(args, "skillName");
            String version = str(args, "version");
            String versionDesc = version.isEmpty() ? "自动递增" : version;

            String text = """
                    请帮我把本地的「%s」Skill 更新到 CodingHub 工具广场，新版本: %s。
                    
                    操作步骤:
                    1. 调用 h3_coding_hub_tool_search 搜索「%s」，找到已发布的 toolId
                    2. 调用 h3_coding_hub_tool_files 获取当前文件列表
                    3. 如需替换文件: 对每个要删除的旧文件调用 h3_coding_hub_tool_file_delete
                       - readme 文件可保留不删
                    4. 调用 h3_coding_hub_tool_modify 更新工具:
                       - toolId: 上一步获取的 ID
                       - version: "%s"（留空则自动递增最后一段版本号）
                       - username/password: 从记忆中获取
                       - 未传入的字段保持不变
                    5. 准备新版本文件（重要）:
                       - 检查 skill 目录的文件数量
                       - 如果包含多个文件（不止 SKILL.md），必须先压缩为 zip 包（保留目录结构）再上传
                       - 只有仅含一个 SKILL.md 文件时才可直接上传
                       - 严禁将多个文件逐个直接上传
                    6. 调用 h3_coding_hub_tool_file_upload 获取上传端点
                    7. 用 curl 上传新版本文件
                    8. 调用 h3_coding_hub_tool_get 确认更新成功
                    9. 将新版本号写入本地 skill 目录的 tools.version 文件
                    """.formatted(skillName, versionDesc, skillName, version);

            return userPromptResult("更新 " + skillName + " 到 " + versionDesc, text);
        });
    }

    // ── 6. 发布插件 ───────────────────────────────────────────────

    private McpServerFeatures.SyncPromptSpecification publishPlugin() {
        McpSchema.Prompt prompt = McpSchema.Prompt.builder("publish-plugin")
                .title("发布插件")
                .description("将本地插件源码制作成符合 CodingHub 规范的插件包（zip），发布到插件市场")
                .arguments(List.of(
                        McpSchema.PromptArgument.builder("name")
                                .description("插件名（kebab-case，如 openspec-pack）")
                                .required(true)
                                .build(),
                        McpSchema.PromptArgument.builder("version")
                                .description("版本号（如 1.0.0）")
                                .required(true)
                                .build(),
                        McpSchema.PromptArgument.builder("description")
                                .description("插件一句话描述（可选）")
                                .required(false)
                                .build(),
                        McpSchema.PromptArgument.builder("source")
                                .description("来源标识（可选，如 owner/repo 或 URL）")
                                .required(false)
                                .build(),
                        McpSchema.PromptArgument.builder("dirPath")
                                .description("本地插件源码目录（可选，用于检查规范后打包）")
                                .required(false)
                                .build()
                ))
                .build();

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, req) -> {
            Map<String, Object> args = req.arguments() != null ? req.arguments() : Map.of();
            String name = str(args, "name");
            String version = str(args, "version");
            String dirPath = str(args, "dirPath");
            String dirDesc = dirPath.isEmpty() ? "由用户指定的目录" : dirPath;

            String text = """
                    请帮我把插件「%s」（版本 %s）制作成符合 CodingHub 规范的插件包并发布到插件市场。

                    【插件包制作规范 — 打包前必须逐条检查】
                    1. zip 根目录必须包含 plugin.json（放在根目录或 .codebuddy-plugin/ 目录下均可）
                    2. plugin.json 字段:
                       - name (必填): 插件名，必须与本次发布的名称一致，kebab-case（小写字母/数字，用 - 分隔）
                       - version (必填): 语义化版本号（如 1.0.0）
                       - description (选填): 一句话描述
                       - icon (选填): 图标
                       - 严禁声明 commands / skills / agents / hooks 字段 —— 平台会自动扫描组件目录并生成摘要，
                         显式声明路径与自动扫描规则不一致会导致组件加载失败（这是常见坑）
                    3. 组件布局（平台自动扫描，无需声明）:
                       - commands/<插件名>.<命令名>.md —— 命令文件必须平铺在 commands/ 目录，用点分命名
                         （如 commands/openspec-pack.new.md），每个文件 frontmatter 含 name 和 description
                       - skills/<技能名>/SKILL.md —— 技能放独立子目录
                       - agents/、hooks/ 同理
                    4. 大小限制: 单个 zip ≤ 50MB，解压后 ≤ 200MB
                    5. 若提供源码目录 %s，请先检查目录结构是否符合上述规范，必要时调整后再打包

                    【发布步骤】
                    1. 调用 h3_coding_hub_plugin_search 搜索「%s」，确认没有重复发布
                    2. 调用 h3_coding_hub_plugin_create 创建插件草稿:
                       - name: %s
                       - version: %s
                       - description: 可选
                       - source: 可选（留空或填 owner/repo / URL）
                       - username/password: 从记忆中获取，或询问用户
                       - 记录返回的 pluginId
                    3. 调用 h3_coding_hub_plugin_file_upload 获取 REST 上传信息（uploadUrl、httpMethod、formFields、limits）
                    4. 用 curl 按返回的 uploadUrl 执行 multipart 上传（zip 内的 name/version 必须与草稿一致）:
                       curl -X POST {uploadUrl} -F "file=@/path/to/%s.zip"
                    5. 上传成功后调用 h3_coding_hub_plugin_search 确认插件已发布且版本为 %s
                    6. 展示发布结果（插件 ID、名称、版本，以及 marketplace 拉取地址）
                    """.formatted(name, version, dirDesc, name, name, version, name, version);

            return userPromptResult("发布插件 " + name + " v" + version + " 到 CodingHub", text);
        });
    }

    // ── 7. 更新插件 ───────────────────────────────────────────────

    private McpServerFeatures.SyncPromptSpecification updatePlugin() {
        McpSchema.Prompt prompt = McpSchema.Prompt.builder("update-plugin")
                .title("更新插件")
                .description("将本地插件的新版本覆盖更新到 CodingHub 插件市场（版本必须递增）")
                .arguments(List.of(
                        McpSchema.PromptArgument.builder("name")
                                .description("要更新的插件名（kebab-case，如 openspec-pack）")
                                .required(true)
                                .build(),
                        McpSchema.PromptArgument.builder("version")
                                .description("新版本号（如 1.0.1），留空则自动递增最后一段")
                                .required(false)
                                .build(),
                        McpSchema.PromptArgument.builder("dirPath")
                                .description("本地插件源码目录（可选，用于重新打包）")
                                .required(false)
                                .build()
                ))
                .build();

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, req) -> {
            Map<String, Object> args = req.arguments() != null ? req.arguments() : Map.of();
            String name = str(args, "name");
            String version = str(args, "version");
            String versionDesc = version.isEmpty() ? "自动递增" : version;
            String dirPath = str(args, "dirPath");
            String dirDesc = dirPath.isEmpty() ? "由用户指定的目录" : dirPath;

            String text = """
                    请帮我把本地插件「%s」的新版本更新到 CodingHub 插件市场。新版本: %s。

                    【更新前必读】
                    - 更新接口要求版本必须变化，版本不变会被拒绝（如 1.0.0 → 1.0.1）
                    - zip 内的 plugin.json 的 name 必须与已发布插件一致
                    - 保持插件包规范: 根目录 plugin.json（不声明 commands/skills/agents/hooks 字段），
                      命令文件平铺在 commands/<插件名>.<命令名>.md

                    【更新步骤】
                    1. 调用 h3_coding_hub_plugin_search 搜索「%s」，找到已发布的 pluginId 和当前版本
                    2. 确定新版本号:
                       - 已指定 %s 则直接使用
                       - 未指定则自动递增最后一段（1.0.0 → 1.0.1；1.0.0-beta → 1.0.1-beta）
                    3. 修改本地源码目录 %s 中 plugin.json 的 version 为新版本，检查组件目录结构后重新打包为 zip
                    4. 用 curl 执行 HTTP PUT multipart 覆盖更新（需认证 token）:
                       curl -X PUT {baseUrl}/api/v1/plugins/{pluginId} \\
                         -H "Authorization: Bearer <token>" -F "file=@/path/to/%s.zip"
                       - 也可先调用 h3_coding_hub_plugin_file_upload 获取上传信息，再按返回的
                         uploadUrl 与 httpMethod 上传
                    5. 更新后调用 h3_coding_hub_plugin_search 确认新版本已生效
                    6. 提示: 插件市场（marketplace.json / marketplace.zip）会自动同步新版本，
                       客户端重新拉取市场即会收到更新
                    """.formatted(name, versionDesc, name, versionDesc, dirDesc, name);

            return userPromptResult("更新插件 " + name + " 到 " + versionDesc, text);
        });
    }

    // ── 8. 论坛发帖 ──────────────────────────────────────────────

    private McpServerFeatures.SyncPromptSpecification forumPost() {
        McpSchema.Prompt prompt = McpSchema.Prompt.builder("forum-post")
                .title("论坛发帖")
                .description("将本地 Markdown 文件或指定内容发布到 CodingHub 论坛")
                .arguments(List.of(
                        McpSchema.PromptArgument.builder("filePath")
                                .description("要发布的本地 Markdown 文件路径")
                                .required(false)
                                .build(),
                        McpSchema.PromptArgument.builder("title")
                                .description("帖子标题（如果不指定文件路径）")
                                .required(false)
                                .build()
                ))
                .build();

        return new McpServerFeatures.SyncPromptSpecification(prompt, (exchange, req) -> {
            Map<String, Object> args = req.arguments() != null ? req.arguments() : Map.of();
            String filePath = str(args, "filePath");
            String title = str(args, "title");

            String sourceDesc;
            if (!filePath.isEmpty()) {
                sourceDesc = "从文件 " + filePath + " 读取内容";
            } else if (!title.isEmpty()) {
                sourceDesc = "标题: " + title;
            } else {
                sourceDesc = "由用户指定内容";
            }

            String text = """
                    请帮我在 CodingHub 论坛发一篇新帖子。来源: %s。
                    
                    操作步骤:
                    1. 准备帖子内容:
                       %s
                       - 如果是文件路径，读取该 Markdown 文件内容作为帖子正文
                       - 如果指定了标题，使用该标题；否则从文件内容提取标题
                    2. 确认 categoryId（论坛分类 ID）:
                       - 可通过 h3_coding_hub_post_search 查看已有帖子的分类来推断
                       - 或询问用户希望发到哪个分类
                    3. 调用 h3_coding_hub_post_create 创建帖子:
                       - title: 帖子标题
                       - content: Markdown 正文
                       - categoryId: 论坛分类 ID
                       - username/password: 从记忆中获取
                    4. 创建成功后调用 h3_coding_hub_post_get 确认帖子内容完整
                    5. 展示发帖结果（帖子 ID、链接）
                    """.formatted(sourceDesc, sourceDesc);

            return userPromptResult("发帖到 CodingHub 论坛", text);
        });
    }

    // ── 辅助方法 ──────────────────────────────────────────────────

    private String str(Map<String, Object> args, String key) {
        Object val = args.get(key);
        return val != null ? String.valueOf(val) : "";
    }

    private McpSchema.GetPromptResult userPromptResult(String description, String text) {
        McpSchema.PromptMessage message = new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(text));
        return new McpSchema.GetPromptResult(description, List.of(message));
    }
}
