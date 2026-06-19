package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.*;
import com.iaihub.toolbox.model.AccountStatus;
import com.iaihub.toolbox.model.Role;
import com.iaihub.toolbox.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/pending-users")
    public ResponseEntity<ApiResponse<List<PendingUserDTO>>> getPendingUsers() {
        List<PendingUserDTO> users = userService.getPendingUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<ApiResponse<ApprovalResponse>> approveUser(@PathVariable Long id) {
        ApprovalResponse response = userService.approveUser(id);
        return ResponseEntity.ok(ApiResponse.success("审批通过", response));
    }

    @PostMapping("/reject/{id}")
    public ResponseEntity<ApiResponse<ApprovalResponse>> rejectUser(@PathVariable Long id) {
        ApprovalResponse response = userService.rejectUser(id);
        return ResponseEntity.ok(ApiResponse.success("已拒绝", response));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserDTO>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {

        Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try {
                roleEnum = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid role filter
            }
        }

        AccountStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = AccountStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // ignore invalid status filter
            }
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminUserDTO> users = userService.getUsers(roleEnum, statusEnum, keyword, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {

        AccountStatus newStatus;
        try {
            newStatus = AccountStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的状态类型");
        }

        userService.updateUserStatus(id, newStatus);
        String message = newStatus == AccountStatus.DISABLED ? "已封禁" : "已解禁";
        return ResponseEntity.ok(ApiResponse.success(message, null));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("已删除", null));
    }
}
