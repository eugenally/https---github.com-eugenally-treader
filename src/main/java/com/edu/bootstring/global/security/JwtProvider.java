package com.edu.bootstring.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 발급과 검증.
 *
 * <p>액세스 토큰만 쓴다. 리프레시 토큰은 저장소와 회수 정책이 따라붙어야 해서
 * 이 프로젝트 범위에서는 만료 시 재로그인으로 갈음한다.
 */
@Slf4j
@Component
public class JwtProvider {

    private final SecretKey key;
    private final long validityMillis;

    public JwtProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.validity-minutes:30}") long validityMinutes) {
        // HS256 은 최소 256bit(32byte) 키를 요구한다. 짧은 값이 들어오면 여기서 바로 터진다.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMillis = validityMinutes * 60_000L;
    }

    public String createToken(Long memberId, String loginId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("loginId", loginId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validityMillis))
                .signWith(key)
                .compact();
    }

    public long getValidityMillis() {
        return validityMillis;
    }

    /** 검증에 성공하면 클레임을, 실패하면 null 을 준다. 필터는 null 을 그냥 '비인증'으로 취급한다. */
    public Claims parse(String token) {
        try {
            return Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("만료된 토큰: {}", e.getMessage());
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("유효하지 않은 토큰: {}", e.getMessage());
            return null;
        }
    }
}
