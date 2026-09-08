package com.edu.bootstring.member;

/**
 * 회원 상태.
 *
 * <p>탈퇴는 물리 삭제가 아니라 {@link #WITHDRAWN} 전환이다 (D3-10).
 * 회원이 남긴 게시글·견적이 참조를 잃으면 안 되기 때문이다.
 */
public enum MemberStatus {

    /** 가입했지만 이메일 인증 전 — 로그인 불가 */
    PENDING,

    ACTIVE,

    /** 장기 미접속 휴면 */
    DORMANT,

    WITHDRAWN
}
