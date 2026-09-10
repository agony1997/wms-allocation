package com.agony.wmsallocation.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT 的簽發與驗證工具
 */
@Slf4j
@Component
public class JwtUtil {

    // 簽章用的對稱密鑰（HMAC 用同一把鑰匙簽與驗）。
    @Value("${jwt.secret}")
    private String secretString;

    // 8 小時（毫秒）
    private final static long EXPIRATION_TIME = 1000 * 60 * 60 * 8;

    /**
     * 把字串密鑰轉成 HS256 演算法要的 SecretKey 物件。
     * 密鑰長度須 >= 32 bytes（256 bit），否則 jjwt 會拋例外。
     */
    private SecretKey getSecretKey() {
        byte[] bytes = secretString.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(bytes);
    }

    /**
     * 登入成功後呼叫：把使用者身份打包成一顆已簽章的 token 字串。
     */
    public String generateToken(String userCode, Map<String, Set<String>> branchRoles) {
        Date now = new Date();
        Date expiryDate = new Date(System.currentTimeMillis() + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(userCode)                 // sub claim：token 的主體，這裡放登入者代號
                .claim("branchRoles", branchRoles)
                .issuedAt(now)                     // iat claim：簽發時間
                .expiration(expiryDate)            // exp claim：過期時間
                .signWith(getSecretKey())          // 用密鑰簽章（產生第三段 Signature）
                .compact();                        // 組成最終的 Header.Payload.Signature 字串
    }

    /**
     * 每個請求進來時呼叫：驗證 token 並取出 Payload（claims）。
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())    // 用同一把密鑰驗簽章（簽章對不上就代表被竄改）
                    .build()
                    .parseSignedClaims(token)      // 解析並驗證：簽章錯、格式錯、已過期都會在這裡拋 JwtException
                    .getPayload();                 // 驗證通過才回傳 Payload
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[Jwt] Token is invalid : {}", e.getMessage());
            return null;
        }
    }

    /**
     * 從已驗證的 claims 取出 branchRoles。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Set<String>> extractBranchRoles(Claims claims) {
        Map<String, ?> raw = claims.get("branchRoles", Map.class);
        if (raw == null) return Map.of();

        return raw.entrySet()
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> Set.copyOf((Collection<String>) entry.getValue())));
    }
}
