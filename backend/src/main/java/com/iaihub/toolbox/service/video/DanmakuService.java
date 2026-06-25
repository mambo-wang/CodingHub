package com.iaihub.toolbox.service.video;

import com.iaihub.toolbox.dto.video.DanmakuDTO;
import com.iaihub.toolbox.dto.video.SendDanmakuRequest;
import com.iaihub.toolbox.exception.UserNotFoundException;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.model.video.Danmaku;
import com.iaihub.toolbox.repository.UserRepository;
import com.iaihub.toolbox.repository.video.DanmakuRepository;
import com.iaihub.toolbox.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DanmakuService {

    private final DanmakuRepository danmakuRepository;
    private final UserRepository userRepository;

    public List<DanmakuDTO> getDanmakuByVideoId(Long videoId) {
        return danmakuRepository.findByVideoIdWithUser(videoId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public DanmakuDTO sendDanmaku(Long videoId, Long userId, SendDanmakuRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String sanitized = XssSanitizer.sanitize(request.getContent());

        Danmaku d = Danmaku.builder()
                .videoId(videoId)
                .user(user)
                .content(sanitized)
                .timeSeconds(request.getTimeSeconds() != null ? request.getTimeSeconds() : 0.0)
                .color(request.getColor() != null ? request.getColor() : "#FFFFFF")
                .danmakuType(request.getDanmakuType() != null ? request.getDanmakuType() : "SCROLL")
                .build();

        d = danmakuRepository.save(d);
        return toDTO(d);
    }

    private DanmakuDTO toDTO(Danmaku d) {
        return DanmakuDTO.builder()
                .id(d.getId())
                .userId(d.getUser().getId())
                .username(d.getUser().getUsername())
                .nickname(d.getUser().getNickname())
                .content(d.getContent())
                .timeSeconds(d.getTimeSeconds())
                .color(d.getColor())
                .danmakuType(d.getDanmakuType())
                .build();
    }
}
