package com.edu.bootstring.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 회원·인증 API 의 요청/응답 형태
 */
public final class MemberDtos {

    private MemberDtos() {
    }

    /** 회원가입. 아이디·비밀번호·이름·이메일·전화번호가 필수이고 부서·직급은 선택이다. */
    public record SignUpRequest(
            @NotBlank(message = "아이디를 입력해 주세요.")
            @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$",
                    message = "아이디는 영문·숫자·밑줄 4~20자로 입력해 주세요.")
            String loginId,

            @NotBlank(message = "비밀번호를 입력해 주세요.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,30}$",
                    message = "비밀번호는 영문과 숫자를 포함해 8자 이상이어야 합니다.")
            String password,

            @NotBlank(message = "비밀번호 확인을 입력해 주세요.")
            String passwordConfirm,

            @NotBlank(message = "이름을 입력해 주세요.")
            @Size(max = 50)
            String name,

            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            @Size(max = 150)
            String email,

            @NotBlank(message = "전화번호를 입력해 주세요.")
            @Pattern(regexp = "^[0-9+\\-() ]{7,30}$", message = "전화번호 형식이 올바르지 않습니다.")
            String phone,

            /** 선택 항목 */
            @Size(max = 50) String dept,
            @Size(max = 50) String position
    ) {
        public boolean passwordMatches() {
            return password != null && password.equals(passwordConfirm);
        }
    }

    public record LoginRequest(
            @NotBlank(message = "아이디를 입력해 주세요.") String loginId,
            @NotBlank(message = "비밀번호를 입력해 주세요.") String password
    ) {
    }

    public record LoginResponse(
            String accessToken,
            long expiresInSeconds,
            MemberResponse member
    ) {
    }

    /** 아이디·이메일 중복확인 응답 */
    public record AvailabilityResponse(
            String value,
            boolean available,
            String message
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "이름을 입력해 주세요.") @Size(max = 50) String name,
            @NotBlank(message = "전화번호를 입력해 주세요.")
            @Pattern(regexp = "^[0-9+\\-() ]{7,30}$", message = "전화번호 형식이 올바르지 않습니다.")
            String phone,
            @NotBlank(message = "이메일을 입력해 주세요.") @Email @Size(max = 150) String email,
            @Size(max = 50) String dept,
            @Size(max = 50) String position
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "현재 비밀번호를 입력해 주세요.") String currentPassword,
            @NotBlank(message = "새 비밀번호를 입력해 주세요.")
            @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,30}$",
                    message = "비밀번호는 영문과 숫자를 포함해 8자 이상이어야 합니다.")
            String newPassword,
            @NotBlank(message = "새 비밀번호 확인을 입력해 주세요.") String newPasswordConfirm
    ) {
        public boolean passwordMatches() {
            return newPassword != null && newPassword.equals(newPasswordConfirm);
        }
    }

    public record FindPasswordRequest(
            @NotBlank(message = "아이디를 입력해 주세요.") String loginId,
            @NotBlank(message = "이메일을 입력해 주세요.") @Email String email
    ) {
    }

    public record MemberResponse(
            Long id,
            String loginId,
            String name,
            String email,
            String phone,
            String role,
            String status,
            String dept,
            String position,
            boolean emailVerified,
            boolean passwordTemporary,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt
    ) {
    }
}
