package com.contied.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParserBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${supabase.jwt.public_key}")
    private String jwtPublicKeyJson;

    private Key publicKey;

    /** 클라이언트 시계 오차 허용 범위 (초) */
    private static final long CLOCK_SKEW_SECONDS = 60L;

    @PostConstruct
    public void init() throws Exception {
        // JWK JSON 파싱
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> jwk = objectMapper.readValue(jwtPublicKeyJson, new TypeReference<Map<String, String>>() {});

        String xBase64 = jwk.get("x");
        String yBase64 = jwk.get("y");

        // Base64URL 디코딩
        byte[] xBytes = Base64.getUrlDecoder().decode(xBase64);
        byte[] yBytes = Base64.getUrlDecoder().decode(yBase64);

        BigInteger x = new BigInteger(1, xBytes);
        BigInteger y = new BigInteger(1, yBytes);

        // EC Public Key 생성 (P-256 / secp256r1)
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new java.security.spec.ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecParameters = params.getParameterSpec(ECParameterSpec.class);

        ECPublicKeySpec keySpec = new ECPublicKeySpec(new ECPoint(x, y), ecParameters);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        this.publicKey = keyFactory.generatePublic(keySpec);
    }

    public boolean validateToken(String token) {
        try {
            buildParser()
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT 만료: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        Claims claims = buildParser()
                .build()
                .parseClaimsJws(token)
                .getBody();
        // exp 클레임 존재 여부 명시 검증
        if (claims.getExpiration() == null) {
            throw new io.jsonwebtoken.MalformedJwtException("JWT에 exp(만료) 클레임이 없습니다.");
        }
        return claims;
    }

    /** 공통 파서 빌더: 서명키 + clock skew 1분 허용 */
    private JwtParserBuilder buildParser() {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .setAllowedClockSkewSeconds(CLOCK_SKEW_SECONDS);
    }
}
