package com.edu.bootstring.global.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 서버 헬스체크 컨트롤러 (M0 배포 검증용)
 */
@Tag(name = "Health Check", description = "시스템 상태 확인 API")
@RestController
@RequestMapping("/api")
public class HealthCheckController {

    @Operation(summary = "헬스체크", description = "서버 정상 구동 및 배포 확인용 API (HTTP 200 반환)")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "treader",
                "timestamp", LocalDateTime.now(),
                "message", "Treader Export Order Management System is healthy"
        ));
    }
}
