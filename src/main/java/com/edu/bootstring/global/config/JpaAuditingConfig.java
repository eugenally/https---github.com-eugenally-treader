package com.edu.bootstring.global.config;

import com.edu.bootstring.global.security.MemberPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing 활성화 및 감사 작성자 자동 주입 설정
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    /** CREATED_BY / UPDATED_BY 컬럼 길이 */
    private static final int AUDITOR_MAX_LENGTH = 50;

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("SYSTEM");
            }
            // Authentication.getName() 은 principal 이 UserDetails 가 아니면 toString() 을 돌려준다.
            // MemberPrincipal 은 레코드라 "MemberPrincipal[memberId=.., loginId=.., role=..]" 가 되어
            // VARCHAR2(50) 을 넘겨 ORA-12899 로 터진다. 로그인 아이디만 꺼내 쓴다.
            String auditor = authentication.getPrincipal() instanceof MemberPrincipal principal
                    ? principal.loginId()
                    : authentication.getName();

            if (auditor == null || auditor.isBlank()) {
                return Optional.of("SYSTEM");
            }
            return Optional.of(auditor.length() > AUDITOR_MAX_LENGTH
                    ? auditor.substring(0, AUDITOR_MAX_LENGTH)
                    : auditor);
        };
    }
}
