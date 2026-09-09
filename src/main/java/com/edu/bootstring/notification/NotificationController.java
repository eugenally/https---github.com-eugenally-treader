package com.edu.bootstring.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * M7 마일스톤: 알림 API 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "알림 API")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 미읽음 알림 목록
     */
    @GetMapping("/unread")
    @Operation(summary = "미읽음 알림 목록", description = "현재 사용자의 미읽음 알림")
    public ResponseEntity<Page<NotificationDto>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // TODO: 현재 사용자 ID 가져오기
        Long memberId = 1L;
        Page<Notification> notifs = notificationService.getUnreadNotifications(memberId, page, size);
        Page<NotificationDto> dtos = notifs.map(NotificationDto::from);
        return ResponseEntity.ok(dtos);
    }

    /**
     * 모든 알림 목록
     */
    @GetMapping
    @Operation(summary = "모든 알림 목록", description = "현재 사용자의 모든 알림 (읽음/미읽음)")
    public ResponseEntity<Page<NotificationDto>> getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // TODO: 현재 사용자 ID 가져오기
        Long memberId = 1L;
        Page<Notification> notifs = notificationService.getAllNotifications(memberId, page, size);
        Page<NotificationDto> dtos = notifs.map(NotificationDto::from);
        return ResponseEntity.ok(dtos);
    }

    /**
     * 미읽음 알림 개수
     */
    @GetMapping("/unread-count")
    @Operation(summary = "미읽음 알림 개수", description = "현재 사용자의 미읽음 알림 개수")
    public ResponseEntity<Long> getUnreadCount() {
        // TODO: 현재 사용자 ID 가져오기
        Long memberId = 1L;
        long count = notificationService.getUnreadCount(memberId);
        return ResponseEntity.ok(count);
    }

    /**
     * 특정 알림 읽음 표시
     */
    @PostMapping("/{notifId}/read")
    @Operation(summary = "알림 읽음 표시")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notifId) {
        notificationService.markAsRead(notifId);
        return ResponseEntity.ok().build();
    }

    /**
     * 모든 미읽음 알림 읽음 표시
     */
    @PostMapping("/mark-all-as-read")
    @Operation(summary = "모든 알림 읽음 표시")
    public ResponseEntity<Void> markAllAsRead() {
        // TODO: 현재 사용자 ID 가져오기
        Long memberId = 1L;
        notificationService.markAllAsRead(memberId);
        return ResponseEntity.ok().build();
    }
}
