package com.edu.bootstring.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * React SPA 클라이언트 라우팅 지원 컨트롤러
 * API 및 정적 자원 외의 모든 URL 요청을 index.html로 포워딩합니다.
 */
@Controller
public class SpaForwardController {

    // [^.]* 로 확장자 있는 경로를 제외한다. index.html 을 포함시키면 웰컴 페이지의
    // forward:/index.html 을 이 컨트롤러가 다시 잡아 무한 포워드에 빠진다.
    @GetMapping(value = {
            "/{path:^(?!api|swagger-ui|v3|static|assets|error)[^.]*}",
            "/{path:^(?!api|swagger-ui|v3|static|assets|error)[^.]*}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
