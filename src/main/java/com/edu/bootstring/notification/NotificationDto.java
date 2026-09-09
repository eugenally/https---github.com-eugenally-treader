package com.edu.bootstring.notification;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 알림 DTO
 */
@Getter
public class NotificationDto {

    private Long id;

    private String notifType;

    private String title;

    private String message;

    private String refType;

    private Long refId;

    private String readYn;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private NotificationDto(Notification notif) {
        this.id = notif.getId();
        this.notifType = notif.getNotifType();
        this.title = notif.getTitle();
        this.message = notif.getMessage();
        this.refType = notif.getRefType();
        this.refId = notif.getRefId();
        this.readYn = notif.getReadYn();
        this.createdAt = notif.getCreatedAt();
    }

    public static NotificationDto from(Notification notif) {
        return new NotificationDto(notif);
    }
}
