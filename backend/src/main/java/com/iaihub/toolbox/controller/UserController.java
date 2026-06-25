package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.AvatarUploadResponse;
import com.iaihub.toolbox.dto.ChangePasswordRequest;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.PublicUserDTO;
import com.iaihub.toolbox.dto.ToolSummaryDTO;
import com.iaihub.toolbox.dto.UpdateProfileRequest;
import com.iaihub.toolbox.dto.UserDTO;
import com.iaihub.toolbox.exception.AvatarValidationException;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.ToolService;
import com.iaihub.toolbox.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ToolService toolService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        UserDTO user = userService.getCurrentUser(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/me/tools")
    public ResponseEntity<ApiResponse<PageResponse<ToolSummaryDTO>>> getMyTools(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        PageResponse<ToolSummaryDTO> response = toolService.getMyTools(
                currentUser.getId(), categoryId, keyword, sortBy, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AvatarUploadResponse> uploadAvatar(
            @RequestParam("avatar") MultipartFile file,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new AvatarValidationException("未登录");
        }
        AvatarUploadResponse response = userService.uploadAvatar(currentUser.getId(), file);
        return ApiResponse.success("头像上传成功", response);
    }

    @DeleteMapping("/me/avatar")
    public ApiResponse<Void> deleteAvatar(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new AvatarValidationException("未登录");
        }
        userService.deleteAvatar(currentUser.getId());
        return ApiResponse.success("头像已移除", null);
    }

    @PutMapping("/me/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal User currentUser) {
        UserDTO updated = userService.updateProfile(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal User currentUser) {
        userService.changePassword(currentUser.getId(), request);
        return ApiResponse.success("密码修改成功", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<PublicUserDTO> getPublicProfile(@PathVariable Long id) {
        return ApiResponse.success(userService.getPublicProfile(id));
    }
}
