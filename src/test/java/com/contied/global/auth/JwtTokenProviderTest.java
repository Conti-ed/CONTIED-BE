package com.contied.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트.
 *
 * @PostConstruct / @Value 우회를 위해 ReflectionTestUtils.setField 로
 * 테스트용 EC 공개키를 직접 주입한다.
 * 실제 Supabase JWK 형식을 흉내내지 않고 단순 EC P-256 키페어를 사용한다.
 */
class JwtTokenProviderTest {

    private static PrivateKey privateKey;
    private static PublicKey publicKey;
    private static JwtTokenProvider provider;

    @BeforeAll
    static void setUpKeys() throws Exception {
        // EC P-256 키페어 생성 (JwtTokenProvider.init()과 동일한 곡선)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        KeyPair keyPair = kpg.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey  = keyPair.getPublic();

        // Spring 컨텍스트 없이 인스턴스 생성 후 publicKey 직접 주입
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "publicKey", publicKey);
    }

    // -------------------------------------------------------------------------
    // 헬퍼: 유효한 토큰 생성 (exp 포함)
    // -------------------------------------------------------------------------
    private String buildValidToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L)) // 1분 후 만료
                .signWith(privateKey, SignatureAlgorithm.ES256)
                .compact();
    }

    // -------------------------------------------------------------------------
    // 헬퍼: 만료된 토큰 생성
    // -------------------------------------------------------------------------
    private String buildExpiredToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("email", email)
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000L))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000L)) // 이미 만료
                .signWith(privateKey, SignatureAlgorithm.ES256)
                .compact();
    }

    // -------------------------------------------------------------------------
    // 헬퍼: exp 없는 토큰 생성
    // -------------------------------------------------------------------------
    private String buildTokenWithoutExp(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("email", email)
                .setIssuedAt(new Date())
                // setExpiration 생략
                .signWith(privateKey, SignatureAlgorithm.ES256)
                .compact();
    }

    // -------------------------------------------------------------------------
    // 테스트 1: 정상 토큰 — validateToken=true, email 추출 가능
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("정상 토큰: validateToken=true이고 getClaims에서 email 추출")
    void validateToken_returnsTrue_forValidToken() {
        String token = buildValidToken("user@test.com");

        boolean valid = provider.validateToken(token);
        assertThat(valid).isTrue();

        Claims claims = provider.getClaims(token);
        assertThat(claims.get("email", String.class)).isEqualTo("user@test.com");
    }

    // -------------------------------------------------------------------------
    // 테스트 2: 만료된 토큰 — validateToken=false (ExpiredJwtException 캐치 확인)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("만료된 토큰: validateToken=false (ExpiredJwtException 처리)")
    void validateToken_returnsFalse_forExpiredToken() {
        // clock skew 가 60초이므로, 만료 시각을 120초 전으로 설정하여
        // CLOCK_SKEW_SECONDS(60) 를 넘기면 ExpiredJwtException 이 던져짐
        String token = buildExpiredToken("user@test.com");

        boolean valid = provider.validateToken(token);
        assertThat(valid).isFalse();
    }

    // -------------------------------------------------------------------------
    // 테스트 3: 서명 오류 토큰 — validateToken=false
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("서명 오류 토큰: validateToken=false")
    void validateToken_returnsFalse_forWrongSignatureToken() throws Exception {
        // 다른 키페어로 서명한 토큰을 provider(공개키가 다른)로 검증 → 실패
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(256);
        KeyPair otherKeyPair = kpg.generateKeyPair();

        String tokenSignedWithOtherKey = Jwts.builder()
                .setSubject("user@test.com")
                .claim("email", "user@test.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(otherKeyPair.getPrivate(), SignatureAlgorithm.ES256)
                .compact();

        boolean valid = provider.validateToken(tokenSignedWithOtherKey);
        assertThat(valid).isFalse();
    }

    // -------------------------------------------------------------------------
    // 테스트 4: exp 클레임 없는 토큰 — getClaims에서 MalformedJwtException 발생
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("exp 클레임 없는 토큰: getClaims에서 예외 발생")
    void getClaims_throwsException_whenExpClaimMissing() {
        String tokenWithoutExp = buildTokenWithoutExp("user@test.com");

        // validateToken 은 true 일 수 있음 (jjwt 는 exp 없는 토큰도 파싱 허용)
        // getClaims 내부의 명시 검증이 MalformedJwtException 을 던져야 함
        assertThatThrownBy(() -> provider.getClaims(tokenWithoutExp))
                .isInstanceOf(io.jsonwebtoken.MalformedJwtException.class)
                .hasMessageContaining("exp(만료) 클레임이 없습니다");
    }
}
