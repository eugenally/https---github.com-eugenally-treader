package com.edu.bootstring.member;

import com.edu.bootstring.global.common.YesNoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 이메일로 보내는 일회용 토큰. 가입 인증과 비밀번호 재설정에 함께 쓴다.
 *
 * <p>한 번 쓰면 {@code used} 가 되고 다시 통과하지 않는다.
 * 유효기한이 지난 토큰도 거절한다.
 */
@Entity
@Getter
@Table(name = "EMAIL_TOKEN")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailToken {

    /** 가입 인증 링크의 유효 시간 */
    public static final long SIGNUP_VALID_HOURS = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TOKEN_ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "TOKEN", nullable = false, length = 100, updatable = false)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "PURPOSE", nullable = false, length = 20)
    private EmailTokenPurpose purpose;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "USED_YN", nullable = false, length = 1)
    private boolean used;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private EmailToken(Long memberId, EmailTokenPurpose purpose, long validHours) {
        this.memberId = memberId;
        this.purpose = purpose;
        this.token = UUID.randomUUID().toString().replace("-", "");
        this.expiresAt = LocalDateTime.now().plusHours(validHours);
        this.used = false;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !used && !isExpired();
    }

    public void markUsed() {
        this.used = true;
    }
}
