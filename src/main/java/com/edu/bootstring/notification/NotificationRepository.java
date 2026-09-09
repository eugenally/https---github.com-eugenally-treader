package com.edu.bootstring.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 알림 저장소
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 회원의 미읽음 알림 목록 (최신순)
     */
    Page<Notification> findByMemberIdAndReadYnOrderByCreatedAtDesc(
        Long memberId, String readYn, Pageable pageable);

    /**
     * 회원의 모든 알림 (최신순)
     */
    Page<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    /**
     * 미읽음 알림 개수
     */
    long countByMemberIdAndReadYn(Long memberId, String readYn);

    /**
     * 미읽음 알림 모두 읽음 표시
     */
    @Query("UPDATE Notification n SET n.readYn = 'Y' WHERE n.memberId = :memberId AND n.readYn = 'N'")
    void markAllAsRead(@Param("memberId") Long memberId);
}
