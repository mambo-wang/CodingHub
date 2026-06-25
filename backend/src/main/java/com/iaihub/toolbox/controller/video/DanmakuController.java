package com.iaihub.toolbox.controller.video;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.video.DanmakuDTO;
import com.iaihub.toolbox.dto.video.SendDanmakuRequest;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.video.DanmakuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/videos/{videoId}/danmaku")
@RequiredArgsConstructor
public class DanmakuController {

    private final DanmakuService danmakuService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DanmakuDTO>>> getDanmaku(
            @PathVariable Long videoId) {
        return ResponseEntity.ok(ApiResponse.success(
                danmakuService.getDanmakuByVideoId(videoId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DanmakuDTO>> sendDanmaku(
            @PathVariable Long videoId,
            @RequestBody SendDanmakuRequest request,
            @AuthenticationPrincipal User currentUser) {
        DanmakuDTO result = danmakuService.sendDanmaku(
                videoId, currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
