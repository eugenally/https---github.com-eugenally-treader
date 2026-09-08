package com.edu.bootstring.member;

/**
 * 이메일 토큰 용도. DDL 의 CK_EMAIL_TOKEN_PURPOSE 제약과 값이 맞아야 한다.
 */
public enum EmailTokenPurpose {

    /** 가입 시 이메일 소유 확인 */
    SIGNUP,

    /** 비밀번호 찾기 — 임시 비밀번호 발급 */
    PWD_RESET
}
