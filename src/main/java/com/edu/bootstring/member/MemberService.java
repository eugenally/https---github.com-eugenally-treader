package com.edu.bootstring.member;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.member.dto.MemberDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 로그인한 회원 자신의 정보 관리.
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Transactional(readOnly = true)
    public MemberDtos.MemberResponse findMe(Long memberId) {
        return AuthService.toResponse(get(memberId));
    }

    /**
     * 회원정보 수정.
     *
     * <p>이메일을 바꾸면 인증 상태가 풀리고 새 주소로 인증 메일이 나간다.
     * 확인되지 않은 주소로 알림이 가면 안 되기 때문이다.
     */
    @Transactional
    public MemberDtos.MemberResponse updateProfile(Long memberId, MemberDtos.UpdateProfileRequest req) {
        Member member = get(memberId);

        boolean emailChanged = !member.getEmail().equalsIgnoreCase(req.email());
        if (emailChanged && memberRepository.existsByEmailAndIdNot(req.email(), memberId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        member.updateProfile(req.name(), req.phone(), req.dept(), req.position());

        if (emailChanged) {
            member.changeEmail(req.email());
            emailTokenRepository.invalidateAll(member.getId(), EmailTokenPurpose.SIGNUP);
            EmailToken token = emailTokenRepository.save(EmailToken.builder()
                    .memberId(member.getId())
                    .purpose(EmailTokenPurpose.SIGNUP)
                    .validHours(EmailToken.SIGNUP_VALID_HOURS)
                    .build());
            mailService.sendSignupVerification(member.getEmail(), member.getName(), token.getToken());
        }

        return AuthService.toResponse(member);
    }

    @Transactional
    public MemberDtos.MemberResponse changePassword(Long memberId, MemberDtos.ChangePasswordRequest req) {
        Member member = get(memberId);

        if (!passwordEncoder.matches(req.currentPassword(), member.getPassword())) {
            throw new BusinessException("현재 비밀번호가 일치하지 않습니다.", ErrorCode.INVALID_PASSWORD);
        }
        if (!req.passwordMatches()) {
            throw new BusinessException("새 비밀번호와 확인이 일치하지 않습니다.", ErrorCode.INVALID_INPUT_VALUE);
        }
        if (passwordEncoder.matches(req.newPassword(), member.getPassword())) {
            throw new BusinessException("현재 비밀번호와 다른 비밀번호를 사용해 주세요.",
                    ErrorCode.INVALID_INPUT_VALUE);
        }

        member.changePassword(passwordEncoder.encode(req.newPassword()));
        return AuthService.toResponse(member);
    }

    /** 탈퇴는 물리 삭제가 아니다 (D3-10). 회원이 남긴 글·거래 문서의 참조가 깨지면 안 된다. */
    @Transactional
    public void withdraw(Long memberId) {
        get(memberId).withdraw();
    }

    /** 관리자 회원 목록 */
    @Transactional(readOnly = true)
    public List<MemberDtos.MemberResponse> findAll() {
        return memberRepository.findAllByOrderByIdDesc().stream()
                .map(AuthService::toResponse)
                .toList();
    }

    private Member get(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND.getMessage()));
    }
}
