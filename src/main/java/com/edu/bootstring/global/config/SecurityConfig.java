package com.edu.bootstring.global.config;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Spring Security 설정.
 *
 * <p>세션을 쓰지 않는다. 인증 상태는 매 요청의 JWT 로만 판단한다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 열어 두는 곳 — 가입·로그인·인증메일 처리와 정적 리소스
                        .requestMatchers(
                                "/api/health",
                                "/api/auth/**",
                                // 게시판은 게시판별 권한(BOARD.AUTH_READ/AUTH_WRITE)이 정하므로
                                // 여기서 일괄로 막지 않고 BoardAccessPolicy 가 판단하게 넘긴다.
                                // 자유게시판은 비회원 읽기·쓰기가 허용된 게시판이다.
                                "/api/boards/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/",
                                "/index.html",
                                "/static/**",
                                "/assets/**",
                                "/*.ico",
                                "/*.svg",
                                "/*.png",
                                "/*.json"
                        ).permitAll()
                        // 업무 API 는 전부 로그인 필요
                        .requestMatchers("/api/**").authenticated()
                        // 나머지(SPA 라우팅 경로)는 프론트가 알아서 처리한다
                        .anyRequest().permitAll()
                )
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(res, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(res, HttpServletResponse.SC_FORBIDDEN, ErrorCode.ACCESS_DENIED))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 인증 실패도 업무 예외와 같은 형태로 내려 줘야 프론트가 한 곳에서 메시지를 꺼낼 수 있다.
     *
     * <p>필드가 셋뿐이라 직렬화기를 끌어들이지 않고 직접 쓴다.
     * Boot 4 는 Jackson 3(tools.jackson)을 쓰므로 버전에 얽히지 않는 편이 낫다.
     */
    private void writeError(HttpServletResponse response, int status, ErrorCode errorCode)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("""
                {"code":"%s","message":"%s","status":%d}"""
                .formatted(errorCode.getCode(), errorCode.getMessage(), status));
    }
}
