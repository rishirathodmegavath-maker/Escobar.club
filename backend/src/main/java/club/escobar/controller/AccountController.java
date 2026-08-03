package club.escobar.controller;

import club.escobar.dto.auth.SetPasswordRequest;
import club.escobar.dto.auth.UserSummaryResponse;
import club.escobar.security.SecurityUser;
import club.escobar.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AuthService authService;

    @PostMapping("/password")
    public ResponseEntity<UserSummaryResponse> setPassword(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody SetPasswordRequest request) {
        return ResponseEntity.ok(authService.setPassword(user.getId(), request));
    }
}
