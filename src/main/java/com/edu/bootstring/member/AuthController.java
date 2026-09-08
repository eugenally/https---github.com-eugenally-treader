package com.edu.bootstring.member;

import com.edu.bootstring.member.dto.MemberDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /** 아이디 중복확인 */
    @GetMapping("/check-login-id")
    public MemberDtos.AvailabilityResponse checkLoginId(@RequestParam String loginId) {
        return authService.checkLoginId(loginId);
    }

    /** 이메일 중복확인 */
    @GetMapping("/check-email")
    public MemberDtos.AvailabilityResponse checkEmail(@RequestParam String email) {
        return authService.checkEmail(email);
    }

    @PostMapping("/signup")
    public MemberDtos.MemberResponse signUp(@Valid @RequestBody MemberDtos.SignUpRequest req) {
        return authService.signUp(req);
    }

    /** 메일의 인증 링크가 호출한다 */
    @PostMapping("/verify-email")
    public MemberDtos.MemberResponse verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestParam String email) {
        authService.resendVerification(email);
        return ResponseEntity.ok(Map.of("message", "인증 메일을 다시 보냈습니다. 메일함을 확인해 주세요."));
    }

    @PostMapping("/login")
    public MemberDtos.LoginResponse login(@Valid @RequestBody MemberDtos.LoginRequest req) {
        return authService.login(req);
    }

    /**
     * 로그아웃.
     *
     * <p>토큰이 무상태라 서버가 회수할 것이 없다. 실제 무효화는 클라이언트가 토큰을 버리는 것으로 끝난다.
     * 즉시 차단이 필요하면 블랙리스트 저장소가 필요한데, 이 프로젝트는 짧은 만료(30분)로 갈음한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "로그아웃되었습니다."));
    }

    @PostMapping("/find-password")
    public ResponseEntity<Map<String, String>> findPassword(
            @Valid @RequestBody MemberDtos.FindPasswordRequest req) {
        authService.issueTemporaryPassword(req);
        return ResponseEntity.ok(Map.of("message", "임시 비밀번호를 이메일로 보냈습니다."));
    }
}
