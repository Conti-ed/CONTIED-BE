package com.contied.global.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final com.contied.user.repository.UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = getJwtFromRequest(request);

        try {
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Claims claims = tokenProvider.getClaims(jwt);
                String email = claims.get("email", String.class);
                
                // metadata가 null일 수 있으므로 안전하게 처리
                Map<String, Object> metadata = (Map<String, Object>) claims.get("user_metadata");
                String nickname = (metadata != null && metadata.get("full_name") != null) 
                        ? (String) metadata.get("full_name") 
                        : "Unknown User";

                System.out.println("Processing JWT for email: " + email);

                // 이메일이 없으면 인증 실패 처리 (또는 다음 필터로)
                if (!StringUtils.hasText(email)) {
                    System.err.println("JWT error: Email is missing in token claims");
                    filterChain.doFilter(request, response);
                    return;
                }

                // 유저 동기화: DB에 없으면 생성
                final String finalEmail = email;
                userRepository.findByEmail(finalEmail).orElseGet(() -> {
                    System.out.println("Creating new user for: " + finalEmail);
                    return userRepository.saveAndFlush(com.contied.user.entity.UserEntity.builder()
                            .email(finalEmail)
                            .nickname(nickname)
                            .role(com.contied.user.entity.Role.UNKNOWN)
                            .build());
                });

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email, null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            System.err.println("JWT Authentication error: " + e.getMessage());
            e.printStackTrace();
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
