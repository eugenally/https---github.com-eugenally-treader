package com.edu.bootstring.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    Optional<EmailToken> findByToken(String token);

    /**
     * 같은 용도의 기존 토큰을 모두 무효화한다.
     * 인증 메일을 다시 보냈을 때 예전 링크가 살아 있으면 안 된다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update EmailToken t set t.used = true
             where t.memberId = :memberId and t.purpose = :purpose and t.used = false
            """)
    int invalidateAll(@Param("memberId") Long memberId, @Param("purpose") EmailTokenPurpose purpose);
}
