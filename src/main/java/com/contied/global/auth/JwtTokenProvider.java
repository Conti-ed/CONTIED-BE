package com.contied.global.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.AlgorithmParameters;
import java.util.Base64;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JwtTokenProvider {

    @Value("${supabase.jwt.public_key}")
    private String jwtPublicKeyJson;

    private Key publicKey;

    @PostConstruct
    public void init() throws Exception {
        // JWK JSON 파싱
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> jwk = objectMapper.readValue(jwtPublicKeyJson, Map.class);
        
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
            Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
