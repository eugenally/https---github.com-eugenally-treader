package com.edu.bootstring.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * M7 마일스톤: 알림 서비스
 */
@Slf4j
@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * 알림 생성
     */
    public Notification createNotification(Long memberId, String notifType, String title,
                                          String message, String refType, Long refId) {
        Notification notif = new Notification(memberId, notifType, title, message, refType, refId);
        return notificationRepository.save(notif);
    }

    /**
     * 회원의 미읽음 알림 목록
     */
    @Transactional(readOnly = true)
    public Page<Notification> getUnreadNotifications(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByMemberIdAndReadYnOrderByCreatedAtDesc(
            memberId, "N", pageable);
    }

    /**
     * 회원의 모든 알림
     */
    @Transactional(readOnly = true)
    public Page<Notification> getAllNotifications(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);
    }

    /**
     * 미읽음 알림 개수
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long memberId) {
        return notificationRepository.countByMemberIdAndReadYn(memberId, "N");
    }

    /**
     * 특정 알림 읽음 표시
     */
    public void markAsRead(Long notifId) {
        notificationRepository.findById(notifId).ifPresent(n -> {
            n.markAsRead();
            notificationRepository.save(n);
        });
    }

    /**
     * 모든 미읽음 알림 읽음 표시
     */
    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllAsRead(memberId);
        log.info("회원 {}의 모든 알림을 읽음 표시했습니다.", memberId);
    }

    /**
     * 납기 임박 알림 생성
     */
    public void notifyDeliveryDue(Long memberId, Long orderId, String orderNo, String productName) {
        String title = "📦 납기 임박";
        String message = String.format("수주 %s의 납기가 7일 남았습니다. (%s)", orderNo, productName);
        createNotification(memberId, "DELIVERY_DUE", title, message, "SALES_ORDER", orderId);
        log.info("납기 임박 알림: 회원 {}, 수주 {}", memberId, orderNo);
    }

    /**
     * 결제 기한 초과 알림 생성
     */
    public void notifyPaymentOverdue(Long memberId, Long invoiceId, String invoiceNo, Long overdueAmount) {
        String title = "⚠️ 결제 기한 초과";
        String message = String.format("인보이스 %s의 결제가 연체되었습니다. (₩%,d)", invoiceNo, overdueAmount);
        createNotification(memberId, "PAYMENT_OVERDUE", title, message, "INVOICE", invoiceId);
        log.info("결제 연체 알림: 회원 {}, 인보이스 {}", memberId, invoiceNo);
    }
}
