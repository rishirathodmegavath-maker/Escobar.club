package club.escobar.controller;

import club.escobar.dto.account.SessionResponse;
import club.escobar.dto.account.TwoFactorConfirmRequest;
import club.escobar.dto.account.TwoFactorDisableRequest;
import club.escobar.dto.account.TwoFactorSetupResponse;
import club.escobar.dto.auth.SetPasswordRequest;
import club.escobar.dto.auth.UserSummaryResponse;
import club.escobar.security.SecurityUser;
import club.escobar.service.AuthService;
import club.escobar.service.SessionService;
import club.escobar.service.TwoFactorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService authService;
    private final TwoFactorService twoFactorService;
    private final SessionService sessionService;

    @PostMapping("/password")
    public ResponseEntity<UserSummaryResponse> setPassword(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody SetPasswordRequest request) {
        return ResponseEntity.ok(authService.setPassword(user.getId(), request));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupResponse> setupTwoFactor(@AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(twoFactorService.setup(user.getId()));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<UserSummaryResponse> confirmTwoFactor(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody TwoFactorConfirmRequest request) {
        return ResponseEntity.ok(twoFactorService.confirm(user.getId(), request));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<UserSummaryResponse> disableTwoFactor(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody TwoFactorDisableRequest request) {
        return ResponseEntity.ok(twoFactorService.disable(user.getId(), request));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> listSessions(@AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(sessionService.list(user.getId()));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revokeSession(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
        sessionService.revoke(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
