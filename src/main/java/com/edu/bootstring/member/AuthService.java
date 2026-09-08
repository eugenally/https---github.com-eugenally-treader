package com.edu.bootstring.member;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.global.security.JwtProvider;
import com.edu.bootstring.member.dto.MemberDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * 가입·인증·로그인을 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int TEMP_PASSWORD_LENGTH = 12;

    private final MemberRepository memberRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final MailService mailService;
    private final SecureRandom random = new SecureRandom();

    // ------------------------------------------------------------------
    // 중복확인
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MemberDtos.AvailabilityResponse checkLoginId(String loginId) {
        if (loginId == null || !loginId.matches("^[a-zA-Z0-9_]{4,20}$")) {
            return new MemberDtos.AvailabilityResponse(loginId, false,
                    "아이디는 영문·숫자·밑줄 4~20자로 입력해 주세요.");
        }
        boolean taken = memberRepository.existsByLoginId(loginId);
        return new MemberDtos.AvailabilityResponse(loginId, !taken,
                taken ? "이미 사용 중인 아이디입니다." : "사용할 수 있는 아이디입니다.");
    }

    @Transactional(readOnly = true)
    public MemberDtos.AvailabilityResponse checkEmail(String email) {
        boolean taken = memberRepository.existsByEmail(email);
        return new MemberDtos.AvailabilityResponse(email, !taken,
                taken ? "이미 가입된 이메일입니다." : "사용할 수 있는 이메일입니다.");
    }

    // ------------------------------------------------------------------
    // 가입 · 이메일 인증
    // ------------------------------------------------------------------

    @Transactional
    public MemberDtos.MemberResponse signUp(MemberDtos.SignUpRequest req) {
        if (!req.passwordMatches()) {
            throw new BusinessException("비밀번호와 비밀번호 확인이 일치하지 않습니다.",
                    ErrorCode.INVALID_INPUT_VALUE);
        }
        // 중복확인 버튼을 눌렀더라도 그 사이 남이 채갈 수 있으므로 저장 직전에 다시 본다.
        if (memberRepository.existsByLoginId(req.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (memberRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = memberRepository.save(Member.builder()
                .loginId(req.loginId())
                .password(passwordEncoder.encode(req.password()))
                .name(req.name())
                .email(req.email())
                .phone(req.phone())
                .role(MemberRole.SALES)      // 화면 가입은 영업담당. 거래처 계정은 관리자가 따로 만든다.
                .dept(req.dept())
                .position(req.position())
                .build());

        issueSignupToken(member);
        return toResponse(member);
    }

    /** 인증 메일 재발송 — 링크를 놓쳤거나 만료됐을 때 쓴다 */
    @Transactional
    public void resendVerification(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("가입되지 않은 이메일입니다."));
        if (member.isEmailVerified()) {
            throw new BusinessException("이미 인증이 완료된 계정입니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        issueSignupToken(member);
    }

    @Transactional
    public MemberDtos.MemberResponse verifyEmail(String token) {
        EmailToken emailToken = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("유효하지 않은 인증 링크입니다.",
                        ErrorCode.INVALID_TOKEN));

        if (emailToken.getPurpose() != EmailTokenPurpose.SIGNUP) {
            throw new BusinessException("가입 인증용 링크가 아닙니다.", ErrorCode.INVALID_TOKEN);
        }
        if (emailToken.isExpired()) {
            throw new BusinessException("인증 링크가 만료되었습니다. 인증 메일을 다시 받아 주세요.",
                    ErrorCode.EXPIRED_TOKEN);
        }
        if (!emailToken.isUsable()) {
            throw new BusinessException("이미 사용된 인증 링크입니다.", ErrorCode.INVALID_TOKEN);
        }

        Member member = memberRepository.findById(emailToken.getMemberId())
                .orElseThrow(() -> new NotFoundException("회원을 찾을 수 없습니다."));

        emailToken.markUsed();
        member.verifyEmail();

        return toResponse(member);
    }

    private void issueSignupToken(Member member) {
        // 새 링크를 보내기 전에 예전 링크를 죽인다
        emailTokenRepository.invalidateAll(member.getId(), EmailTokenPurpose.SIGNUP);

        EmailToken token = emailTokenRepository.save(EmailToken.builder()
                .memberId(member.getId())
                .purpose(EmailTokenPurpose.SIGNUP)
                .validHours(EmailToken.SIGNUP_VALID_HOURS)
                .build());

        mailService.sendSignupVerification(member.getEmail(), member.getName(), token.getToken());
    }

    // ------------------------------------------------------------------
    // 로그인
    // ------------------------------------------------------------------

    @Transactional
    public MemberDtos.LoginResponse login(MemberDtos.LoginRequest req) {
        Member member = memberRepository.findByLoginId(req.loginId())
                // 아이디가 없는지 비밀번호가 틀린지 알려주지 않는다. 계정 존재 여부가 새어 나가면 안 된다.
                .orElseThrow(() -> new BusinessException("아이디 또는 비밀번호가 올바르지 않습니다.",
                        ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(req.password(), member.getPassword())) {
            throw new BusinessException("아이디 또는 비밀번호가 올바르지 않습니다.", ErrorCode.UNAUTHORIZED);
        }
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException("탈퇴한 계정입니다.", ErrorCode.ACCESS_DENIED);
        }
        if (!member.isEmailVerified()) {
            throw new BusinessException("이메일 인증이 완료되지 않았습니다. 메일함을 확인해 주세요.",
                    ErrorCode.ACCESS_DENIED);
        }
        if (!member.canLogin()) {
            throw new BusinessException("로그인할 수 없는 계정 상태입니다. (%s)".formatted(member.getStatus()),
                    ErrorCode.ACCESS_DENIED);
        }

        member.recordLogin();

        String accessToken = jwtProvider.createToken(
                member.getId(), member.getLoginId(), member.getRole().name());

        return new MemberDtos.LoginResponse(
                accessToken,
                jwtProvider.getValidityMillis() / 1000,
                toResponse(member));
    }

    // ------------------------------------------------------------------
    // 비밀번호 찾기
    // ------------------------------------------------------------------

    /** 아이디와 이메일이 모두 맞아야 임시 비밀번호를 보낸다 */
    @Transactional
    public void issueTemporaryPassword(MemberDtos.FindPasswordRequest req) {
        Member member = memberRepository.findByLoginId(req.loginId())
                .filter(m -> m.getEmail().equalsIgnoreCase(req.email()))
                .orElseThrow(() -> new NotFoundException("아이디와 이메일이 일치하는 계정이 없습니다."));

        String temporary = randomPassword();
        member.assignTemporaryPassword(passwordEncoder.encode(temporary));

        mailService.sendTemporaryPassword(member.getEmail(), member.getName(), temporary);
    }

    private String randomPassword() {
        StringBuilder sb = new StringBuilder(TEMP_PASSWORD_LENGTH);
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            sb.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    static MemberDtos.MemberResponse toResponse(Member m) {
        return new MemberDtos.MemberResponse(
                m.getId(),
                m.getLoginId(),
                m.getName(),
                m.getEmail(),
                m.getPhone(),
                m.getRole().name(),
                m.getStatus().name(),
                m.getDept(),
                m.getPosition(),
                m.isEmailVerified(),
                m.isPasswordTemporary(),
                m.getLastLoginAt(),
                m.getCreatedAt());
    }
}
