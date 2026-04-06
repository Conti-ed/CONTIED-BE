package com.contied.user.controller;

import com.contied.user.dto.PostUserRoleDto;
import com.contied.user.dto.UserInfoResponse;
import com.contied.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "사용자 관리 및 역할 설정 API")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "사용자 정보 조회")
    public UserInfoResponse getUserInfo(@AuthenticationPrincipal String email) {
        return userService.getUserInfo(email);
    }

    @GetMapping("/role")
    @Operation(summary = "사용자 역할 조회")
    public Map<String, String> getUserRole(@AuthenticationPrincipal String email) {
        return Map.of("role", userService.getUserRole(email));
    }

    @PostMapping("/role")
    @Operation(summary = "사용자 역할 설정")
    public Map<String, String> postUserRole(
            @AuthenticationPrincipal String email,
            @RequestBody PostUserRoleDto dto
    ) {
        userService.updateUserRole(email, dto);
        return Map.of("success", "true", "role", dto.getRole());
    }

    @GetMapping("/nickname")
    @Operation(summary = "사용자 닉네임 조회")
    public Map<String, String> getUserNickname(@AuthenticationPrincipal String email) {
        return Map.of("nickname", userService.getUserNickname(email));
    }
}
