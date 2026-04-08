package com.contied.user.service;

import com.contied.user.dto.PostUserRoleDto;
import com.contied.user.dto.UserInfoResponse;
import com.contied.user.entity.Role;
import com.contied.user.entity.UserEntity;
import com.contied.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserInfoResponse getUserInfo(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
        
        return UserInfoResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public String getUserRole(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
        return user.getRole().name();
    }

    public String getUserNickname(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
        return user.getNickname();
    }

    @Transactional
    public void updateUserRole(String email, PostUserRoleDto dto) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
        
        try {
            Role role = Role.valueOf(dto.getRole().toUpperCase());
            user.setRole(role);
            userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("유효하지 않은 역할입니다: " + dto.getRole());
        }
    }
}
