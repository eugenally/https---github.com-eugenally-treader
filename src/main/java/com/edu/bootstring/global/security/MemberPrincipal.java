package com.edu.bootstring.global.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 인증된 요청에 실려 다니는 최소 정보. 토큰에서 그대로 꺼낸 값이라 DB 조회가 필요 없다.
 */
public record MemberPrincipal(Long memberId, String loginId, String role) {

    /** 현재 요청의 로그인 회원. 비로그인 요청이면 null 이다. */
    public static MemberPrincipal current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof MemberPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
