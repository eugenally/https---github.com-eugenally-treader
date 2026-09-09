package com.edu.bootstring.notification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 (납기 임박, 연체 판정 등)
 */
@Entity
@Getter
@Table(name = "NOTIFICATION")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqNotification")
    @SequenceGenerator(name = "seqNotification", sequenceName = "SEQ_NOTIFICATION", allocationSize = 1)
    @Column(name = "NOTIF_ID")
    private Long id;

    @Column(name = "MEMBER_ID", nullable = false)
    private Long memberId;

    @Column(name = "NOTIF_TYPE", nullable = false, length = 30)
    private String notifType;  // DELIVERY_DUE, PAYMENT_OVERDUE, QNA_ANSWERED

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "MESSAGE", length = 1000)
    private String message;

    @Column(name = "REF_TYPE", length = 20)
    private String refType;  // SALES_ORDER, INVOICE

    @Column(name = "REF_ID")
    private Long refId;

    @Column(name = "READ_YN", nullable = false, length = 1)
    private String readYn = "N";

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    public Notification(Long memberId, String notifType, String title, String message,
                       String refType, Long refId) {
        this.memberId = memberId;
        this.notifType = notifType;
        this.title = title;
        this.message = message;
        this.refType = refType;
        this.refId = refId;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead() {
        this.readYn = "Y";
    }
}
