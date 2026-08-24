package com.agony.wmsallocation.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// JWT 的簽發與驗證工具。JWT 結構為 Header.Payload.Signature 三段（以 . 分隔）：
// Header/Payload 只是 Base64 編碼（可被任何人解讀，不能放密碼），
// Signature 才是用密鑰簽出來的防偽章——別人沒有密鑰就無法竄改 Payload 後重簽。
@Component
public class JwtUtil {

    // 簽章用的對稱密鑰（HMAC 用同一把鑰匙簽與驗）。
    @Value("${jwt.secret}")
    private String secretString;

    // 把字串密鑰轉成 HS256 演算法要的 SecretKey 物件。
    // 密鑰長度須 >= 32 bytes（256 bit），否則 jjwt 會拋例外。
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
    }

    // 登入成功後呼叫：把使用者身份打包成一顆已簽章的 token 字串。
    public String generateToken(String userCode, String role) {
        long expirationTimeMs = 1000 * 60 * 60 * 8; // 8 小時（毫秒）

        return Jwts.builder()
                .subject(userCode)                 // sub claim：token 的主體，這裡放登入者代號
                .claim("role", role)         // 自訂 claim：角色，之後攔截器據此做權限判斷
                .issuedAt(new Date())              // iat claim：簽發時間
                .expiration(new Date(System.currentTimeMillis() + expirationTimeMs)) // exp claim：過期時間
                .signWith(getSecretKey())          // 用密鑰簽章（產生第三段 Signature）
                .compact();                        // 組成最終的 Header.Payload.Signature 字串
    }

    // 每個請求進來時呼叫：驗證 token 並取出 Payload（claims）。
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())    // 用同一把密鑰驗簽章（簽章對不上就代表被竄改）
                    .build()
                    .parseSignedClaims(token)      // 解析並驗證：簽章錯、格式錯、已過期都會在這裡拋 JwtException
                    .getPayload();                 // 驗證通過才回傳 Payload
        } catch (JwtException | IllegalArgumentException e) {
            // 驗證失敗（簽章不符 / 過期 / 格式錯），或 token 為空字串
            //（jjwt 對空字串丟的是 IllegalArgumentException，不是 JwtException，得一併接住才不會冒成 500）
            // 統一回 null，由呼叫端決定回 401
            return null; // Token 無效或已過期
        }
    }
}
