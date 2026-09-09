package com.edu.bootstring.member;

import com.edu.bootstring.global.common.BaseTimeEntity;
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

/**
 * 회원.
 *
 * <p>가입 직후에는 {@link MemberStatus#PENDING} 이고 이메일 인증을 마쳐야 ACTIVE 가 된다.
 * 인증 전에는 로그인할 수 없다.
 */
@Entity
@Getter
@Table(name = "MEMBER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MEMBER_ID")
    private Long id;

    @Column(name = "LOGIN_ID", nullable = false, length = 50, updatable = false)
    private String loginId;

    /** BCrypt 해시. 평문은 어디에도 남기지 않는다. */
    @Column(name = "PASSWORD", nullable = false, length = 100)
    private String password;

    @Column(name = "NAME", nullable = false, length = 50)
    private String name;

    @Column(name = "EMAIL", nullable = false, length = 150)
    private String email;

    @Column(name = "PHONE", nullable = false, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE", nullable = false, length = 20)
    private MemberRole role;

    /** ROLE_CUSTOMER 인 경우 소속 거래처. 그 외에는 비어 있다. */
    @Column(name = "CUSTOMER_ID")
    private Long customerId;

    @Column(name = "DEPT", length = 50)
    private String dept;

    @Column(name = "POSITION", length = 50)
    private String position;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "EMAIL_VERIFIED_YN", nullable = false, length = 1)
    private boolean emailVerified;

    /** 임시 비밀번호로 로그인한 상태. 다음 로그인 때 변경을 강제한다. */
    @Convert(converter = YesNoConverter.class)
    @Column(name = "PWD_TEMP_YN", nullable = false, length = 1)
    private boolean passwordTemporary;

    @Column(name = "PWD_CHANGED_AT")
    private LocalDateTime passwordChangedAt;

    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private MemberStatus status;

    @Builder
    private Member(String loginId, String password, String name, String email, String phone,
                   MemberRole role, String dept, String position) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role != null ? role : MemberRole.CUSTOMER;
        this.dept = dept;
        this.position = position;
        this.emailVerified = false;
        this.passwordTemporary = false;
        this.status = MemberStatus.PENDING;
        this.passwordChangedAt = LocalDateTime.now();
    }

    /** 이메일 인증 완료 — 여기서 비로소 로그인할 수 있게 된다 */
    public void verifyEmail() {
        this.emailVerified = true;
        if (this.status == MemberStatus.PENDING) {
            this.status = MemberStatus.ACTIVE;
        }
    }

    public void updateProfile(String name, String phone, String dept, String position) {
        this.name = name;
        this.phone = phone;
        this.dept = dept;
        this.position = position;
    }

    /** 이메일을 바꾸면 인증을 다시 받아야 한다. 확인되지 않은 주소로 알림이 나가면 안 되기 때문이다. */
    public void changeEmail(String email) {
        this.email = email;
        this.emailVerified = false;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordTemporary = false;
        this.passwordChangedAt = LocalDateTime.now();
    }

    public void assignTemporaryPassword(String encodedPassword) {
        this.password = encodedPassword;
        this.passwordTemporary = true;
        this.passwordChangedAt = LocalDateTime.now();
    }

    public void recordLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void withdraw() {
        this.status = MemberStatus.WITHDRAWN;
    }

    public boolean canLogin() {
        return status == MemberStatus.ACTIVE && emailVerified;
    }
}
