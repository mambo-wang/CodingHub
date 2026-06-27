package com.iaihub.toolbox.service.kb;

import com.iaihub.toolbox.dto.kb.*;
import com.iaihub.toolbox.exception.DuplicateResourceException;
import com.iaihub.toolbox.exception.ForbiddenException;
import com.iaihub.toolbox.exception.ResourceNotFoundException;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.kb.KbDocument;
import com.iaihub.toolbox.model.kb.KbStatus;
import com.iaihub.toolbox.model.kb.KnowledgeBase;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.kb.KbDocumentRepository;
import com.iaihub.toolbox.repository.kb.KnowledgeBaseRepository;
import com.iaihub.toolbox.service.RagApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KbDocumentRepository kbDocumentRepository;
    private final UserRepository userRepository;
    private final RagApiClient ragApiClient;

    // ── Knowledge Base CRUD ──────────────────────────────────

    @Transactional(readOnly = true)
    public Page<KbResponse> listKnowledgeBases(int page, int size, String sortBy) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        Page<KnowledgeBase> kbPage;

        if ("hot".equals(sortBy)) {
            kbPage = knowledgeBaseRepository.findByStatusOrderByHot(KbStatus.NORMAL, pageable);
        } else {
            kbPage = knowledgeBaseRepository.findByStatusOrderByCreatedAtDesc(KbStatus.NORMAL, pageable);
        }

        return kbPage.map(this::toKbResponse);
    }

    @Transactional(readOnly = true)
    public KbResponse getKnowledgeBase(Long id) {
        KnowledgeBase kb = findActiveKb(id);
        return toKbResponse(kb);
    }

    @Transactional
    public KbResponse createKnowledgeBase(KbCreateRequest request, User user) {
        // Check name uniqueness
        if (knowledgeBaseRepository.existsByNameAndStatus(request.getName(), KbStatus.NORMAL)) {
            throw new DuplicateResourceException("知识库名称已存在");
        }

        // Create MySQL record
        KnowledgeBase kb = KnowledgeBase.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(user.getId())
                .ragCollection(request.getName())
                .status(KbStatus.NORMAL)
                .build();
        kb = knowledgeBaseRepository.save(kb);

        // Initialize RAG collection config
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("chunk_mode", request.getChunkMode() != null ? request.getChunkMode() : "structural");
        config.put("chunk_size", request.getChunkSize() != null ? request.getChunkSize() : 800);
        config.put("chunk_overlap", request.getChunkOverlap() != null ? request.getChunkOverlap() : 50);
        config.put("rerank", request.getRerank() != null ? request.getRerank() : true);
        config.put("description", request.getDescription() != null ? request.getDescription() : "");

        try {
            ragApiClient.configureCollection(kb.getRagCollection(), config);
        } catch (Exception e) {
            log.warn("RAG config failed during creation (tolerated): {}", e.getMessage());
        }

        return toKbResponse(kb);
    }

    @Transactional
    public KbResponse updateKnowledgeBase(Long id, KbUpdateRequest request, User user) {
        KnowledgeBase kb = findActiveKb(id);
        checkOwnerOrAdmin(kb, user);

        // Check name uniqueness if changing name
        if (request.getName() != null && !request.getName().equals(kb.getName())) {
            if (knowledgeBaseRepository.existsByNameAndStatus(request.getName(), KbStatus.NORMAL)) {
                throw new DuplicateResourceException("知识库名称已存在");
            }
            // Update RAG collection name reference (note: RAG collection itself keeps old name)
            kb.setName(request.getName());
        }

        if (request.getDescription() != null) {
            kb.setDescription(request.getDescription());
            // Sync description to RAG config
            try {
                ragApiClient.configureCollection(kb.getRagCollection(),
                        Map.of("description", request.getDescription()));
            } catch (Exception e) {
                log.warn("RAG config sync failed (tolerated): {}", e.getMessage());
            }
        }

        kb = knowledgeBaseRepository.save(kb);
        return toKbResponse(kb);
    }

    @Transactional
    public void deleteKnowledgeBase(Long id, User user) {
        KnowledgeBase kb = findActiveKb(id);
        checkOwnerOrAdmin(kb, user);

        kb.setStatus(KbStatus.DELETED);
        knowledgeBaseRepository.save(kb);

        // Delete RAG collection (best effort)
        ragApiClient.deleteCollection(kb.getRagCollection());
    }

    // ── Document Management ──────────────────────────────────

    @Transactional(readOnly = true)
    public List<KbDocumentResponse> listDocuments(Long kbId) {
        findActiveKb(kbId); // verify exists
        List<KbDocument> docs = kbDocumentRepository.findByKbIdAndStatusOrderByCreatedAtDesc(kbId, KbStatus.NORMAL);
        return docs.stream().map(this::toDocResponse).collect(Collectors.toList());
    }

    @Transactional
    public KbDocumentResponse uploadDocument(Long kbId, MultipartFile file, User user) {
        KnowledgeBase kb = findActiveKb(kbId);
        checkOwnerOrAdmin(kb, user);

        // Upload to RAG
        Map<String, Object> result = ragApiClient.uploadDocument(
                kb.getRagCollection(), file, null, null);

        int chunks = 0;
        if (result.containsKey("chunks")) {
            chunks = ((Number) result.get("chunks")).intValue();
        }

        // Save to MySQL
        KbDocument doc = KbDocument.builder()
                .kbId(kbId)
                .uploaderId(user.getId())
                .originalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed")
                .fileSize(file.getSize())
                .chunkCount(chunks)
                .chunkMode(result.containsKey("chunk_mode") ? (String) result.get("chunk_mode") : null)
                .status(KbStatus.NORMAL)
                .build();
        doc = kbDocumentRepository.save(doc);

        return toDocResponse(doc);
    }

    @Transactional
    public void deleteDocument(Long kbId, Long docId, User user) {
        KnowledgeBase kb = findActiveKb(kbId);
        checkOwnerOrAdmin(kb, user);

        KbDocument doc = kbDocumentRepository.findByIdAndKbIdAndStatus(docId, kbId, KbStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在", docId));

        doc.setStatus(KbStatus.DELETED);
        kbDocumentRepository.save(doc);

        // Delete from RAG (best effort)
        try {
            ragApiClient.deleteDocument(kb.getRagCollection(), doc.getOriginalName());
        } catch (Exception e) {
            log.warn("RAG deleteDocument failed (tolerated): {}", e.getMessage());
        }
    }

    // ── Search ───────────────────────────────────────────────

    public List<KbSearchResultResponse> search(Long kbId, KbSearchRequest request) {
        KnowledgeBase kb = findActiveKb(kbId);

        List<Map<String, Object>> results = ragApiClient.search(
                kb.getRagCollection(),
                request.getQuery(),
                request.getTopK(),
                request.getRerank(),
                request.getExpandContext() != null ? request.getExpandContext() : 0
        );

        return results.stream().map(r -> KbSearchResultResponse.builder()
                .text((String) r.getOrDefault("text", ""))
                .source((String) r.getOrDefault("source", ""))
                .score(r.get("score") instanceof Number ? ((Number) r.get("score")).doubleValue() : 0.0)
                .chunkIndex(r.get("chunkIndex") instanceof Number ? ((Number) r.get("chunkIndex")).intValue() : 0)
                .build()
        ).collect(Collectors.toList());
    }

    // ── Config ───────────────────────────────────────────────

    public Map<String, Object> getConfig(Long kbId) {
        KnowledgeBase kb = findActiveKb(kbId);
        return ragApiClient.getCollectionConfig(kb.getRagCollection());
    }

    @Transactional
    public Map<String, Object> updateConfig(Long kbId, KbConfigRequest request, User user) {
        KnowledgeBase kb = findActiveKb(kbId);
        checkOwnerOrAdmin(kb, user);

        Map<String, Object> config = new LinkedHashMap<>();
        if (request.getChunkMode() != null) config.put("chunk_mode", request.getChunkMode());
        if (request.getChunkSize() != null) config.put("chunk_size", request.getChunkSize());
        if (request.getChunkOverlap() != null) config.put("chunk_overlap", request.getChunkOverlap());
        if (request.getRerank() != null) config.put("rerank", request.getRerank());
        if (request.getDescription() != null) config.put("description", request.getDescription());

        Map<String, Object> result = ragApiClient.configureCollection(kb.getRagCollection(), config);

        // Sync description to MySQL if changed
        if (request.getDescription() != null) {
            kb.setDescription(request.getDescription());
            knowledgeBaseRepository.save(kb);
        }

        return result;
    }

    // ── Helpers ──────────────────────────────────────────────

    private KnowledgeBase findActiveKb(Long id) {
        return knowledgeBaseRepository.findByIdAndStatus(id, KbStatus.NORMAL)
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在", id));
    }

    private void checkOwnerOrAdmin(KnowledgeBase kb, User user) {
        boolean isOwner = kb.getOwnerId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权操作此知识库");
        }
    }

    private KbResponse toKbResponse(KnowledgeBase kb) {
        String ownerNickname = userRepository.findById(kb.getOwnerId())
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("未知用户");

        long docCount = kbDocumentRepository.countByKbIdAndStatus(kb.getId(), KbStatus.NORMAL);

        return KbResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .ownerId(kb.getOwnerId())
                .ownerNickname(ownerNickname)
                .documentCount(docCount)
                .ragCollection(kb.getRagCollection())
                .createdAt(kb.getCreatedAt())
                .build();
    }

    private KbDocumentResponse toDocResponse(KbDocument doc) {
        String uploaderNickname = userRepository.findById(doc.getUploaderId())
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("未知用户");

        return KbDocumentResponse.builder()
                .id(doc.getId())
                .kbId(doc.getKbId())
                .originalName(doc.getOriginalName())
                .fileSize(doc.getFileSize())
                .chunkCount(doc.getChunkCount())
                .chunkMode(doc.getChunkMode())
                .uploaderNickname(uploaderNickname)
                .createdAt(doc.getCreatedAt())
                .build();
    }
}
