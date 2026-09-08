package com.edu.bootstring.member;

/**
 * 회원 역할 (D3-05).
 *
 * <p>Spring Security 의 권한 문자열은 {@code ROLE_} 접두사를 붙여 쓴다.
 * DB 에는 접두사 없이 저장하고, 인증 필터에서만 붙인다.
 */
public enum MemberRole {

    /** 시스템 관리자 — 회원 관리, 마스터 데이터 관리 */
    ADMIN,

    /** 영업담당 — 견적·수주·출하·인보이스 전 과정 */
    SALES,

    /** 거래처 — 자기 거래 건만 조회 */
    CUSTOMER;

    public String authority() {
        return "ROLE_" + name();
    }
}
