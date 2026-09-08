package com.edu.bootstring.global.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * React SPA 클라이언트 라우팅 지원 컨트롤러
 * API 및 정적 자원 외의 모든 URL 요청을 index.html로 포워딩합니다.
 */
@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/{path:^(?!api|swagger-ui|v3|static|assets|favicon\\.ico).*}",
            "/{path:^(?!api|swagger-ui|v3|static|assets).*}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
