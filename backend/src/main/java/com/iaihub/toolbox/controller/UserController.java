package com.iaihub.toolbox.controller;

import com.iaihub.toolbox.dto.ApiResponse;
import com.iaihub.toolbox.dto.PageResponse;
import com.iaihub.toolbox.dto.ToolSummaryDTO;
import com.iaihub.toolbox.dto.UserDTO;
import com.iaihub.toolbox.model.User;
import com.iaihub.toolbox.service.ToolService;
import com.iaihub.toolbox.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
