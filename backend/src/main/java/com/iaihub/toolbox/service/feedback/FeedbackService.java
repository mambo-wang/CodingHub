package com.iaihub.toolbox.service.feedback;

import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.feedback.FeedbackCreateRequest;
import com.iaihub.toolbox.dto.feedback.FeedbackDTO;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.feedback.FeedbackCategory;
import com.iaihub.toolbox.model.feedback.FeedbackMessage;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.feedback.FeedbackMessageRepository;
import com.iaihub.toolbox.util.XssSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackMessageRepository feedbackMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public FeedbackDTO submit(FeedbackCreateRequest request, HttpServletRequest httpRequest) {
        User user = getCurrentUser();
        String ipHash = null;

        FeedbackMessage.FeedbackMessageBuilder builder = FeedbackMessage.builder()
            .content(XssSanitizer.sanitize(request.content()))
            .nickname(XssSanitizer.sanitize(request.nickname()))
            .contact(XssSanitizer.sanitize(request.contact()));

        // 分类处理
        if (request.category() != null && !request.category().isBlank()) {
            try {
                builder.category(FeedbackCategory.valueOf(request.category()));
            } catch (IllegalArgumentException e) {
                builder.category(FeedbackCategory.SUGGESTION);
            }
        }

        if (user != null) {
            // 已登录用户：关联 userId，自动取 nickname
            builder.user(user);
            if (request.nickname() == null || request.nickname().isBlank()) {
                builder.nickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        } else {
            // 匿名用户：计算 ipHash
            ipHash = computeIpHash(httpRequest);
            builder.ipHash(ipHash);
        }

        FeedbackMessage saved = feedbackMessageRepository.save(builder.build());
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackDTO> list(String category, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<FeedbackMessage> result;

        if (category != null && !category.isBlank()) {
            try {
                FeedbackCategory cat = FeedbackCategory.valueOf(category);
                result = feedbackMessageRepository.findByCategoryAndStatusOrderByCreatedAtDesc(
                    cat, FeedbackMessage.Status.NORMAL, pageRequest);
            } catch (IllegalArgumentException e) {
                // Invalid category, return empty
                return PageResponse.<FeedbackDTO>builder()
                    .content(List.of())
                    .totalElements(0)
                    .totalPages(0)
                    .page(page)
                    .size(size)
                    .build();
            }
        } else {
            result = feedbackMessageRepository.findByStatusOrderByCreatedAtDesc(
                FeedbackMessage.Status.NORMAL, pageRequest);
        }

        return PageResponse.<FeedbackDTO>builder()
            .content(result.getContent().stream().map(this::toDTO).toList())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .page(result.getNumber())
            .size(result.getSize())
            .build();
    }

    @Transactional
    public FeedbackDTO reply(Long id, String adminReply, User admin) {
        FeedbackMessage message = feedbackMessageRepository.findByIdAndStatusNormal(id)
            .orElseThrow(() -> new com.iaihub.toolbox.exception.ResourceNotFoundException("留言不存在"));

        message.setAdminReply(XssSanitizer.sanitize(adminReply));
        message.setRepliedBy(admin);
        message.setRepliedAt(LocalDateTime.now());

        FeedbackMessage saved = feedbackMessageRepository.save(message);
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        FeedbackMessage message = feedbackMessageRepository.findByIdAndStatusNormal(id)
            .orElseThrow(() -> new com.iaihub.toolbox.exception.ResourceNotFoundException("留言不存在"));
        message.setStatus(FeedbackMessage.Status.DELETED);
        feedbackMessageRepository.save(message);
    }

    // ==================== HELPERS ====================

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return (User) auth.getPrincipal();
        }
        return null;
    }

    private String computeIpHash(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            ip = forwarded.split(",")[0].trim();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return ip;
        }
    }

    private FeedbackDTO toDTO(FeedbackMessage message) {
        return new FeedbackDTO(
            message.getId(),
            message.getContent(),
            message.getNickname(),
            message.getContact(),
            message.getCategory().name(),
            message.getCreatedAt(),
            message.getAdminReply(),
            message.getRepliedAt()
        );
    }
}
