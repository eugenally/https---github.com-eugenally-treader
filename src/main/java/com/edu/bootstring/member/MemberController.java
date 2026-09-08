package com.edu.bootstring.member;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.security.MemberPrincipal;
import com.edu.bootstring.member.dto.MemberDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    /** 내 정보 */
    @GetMapping("/me")
    public MemberDtos.MemberResponse me() {
        return memberService.findMe(currentMemberId());
    }

    /** 회원정보 수정 */
    @PutMapping("/me")
    public MemberDtos.MemberResponse updateMe(@Valid @RequestBody MemberDtos.UpdateProfileRequest req) {
        return memberService.updateProfile(currentMemberId(), req);
    }

    @PutMapping("/me/password")
    public MemberDtos.MemberResponse changePassword(
            @Valid @RequestBody MemberDtos.ChangePasswordRequest req) {
        return memberService.changePassword(currentMemberId(), req);
    }

    @PostMapping("/me/withdraw")
    public ResponseEntity<Map<String, String>> withdraw() {
        memberService.withdraw(currentMemberId());
        return ResponseEntity.ok(Map.of("message", "탈퇴 처리되었습니다."));
    }

    /** 관리자 회원 목록 */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<MemberDtos.MemberResponse> findAll() {
        return memberService.findAll();
    }

    private Long currentMemberId() {
        MemberPrincipal principal = MemberPrincipal.current();
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principal.memberId();
    }
}
