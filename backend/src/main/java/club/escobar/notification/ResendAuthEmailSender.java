package club.escobar.notification;

import club.escobar.config.EmailProperties;
import club.escobar.config.ResendProperties;
import club.escobar.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Sends real emails via Resend's HTTP API (https://resend.com/docs/api-reference/emails/send-email).
 * A misconfigured or unreachable provider must never block account creation or password-reset
 * requests, so send failures are caught and logged rather than propagated to the caller.
 */
@Service
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "resend")
@RequiredArgsConstructor
public class ResendAuthEmailSender implements AuthEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendAuthEmailSender.class);

    private final RestClient resendRestClient;
    private final EmailProperties emailProperties;
    private final ResendProperties resendProperties;

    @Override
    public void sendPasswordResetEmail(User user, String resetLink) {
        String html = """
                <p>Hello,</p>
                <p>Click the button below to reset your Escobar.Club password.</p>
                <p><a href="%s">Reset password</a></p>
                <p>This link will expire soon. If you didn't request this, you can ignore this email.</p>
                """.formatted(resetLink);
        send(user.getEmail(), "Reset your Escobar.Club password", html);
    }

    @Override
    public void sendVerificationEmail(User user, String verifyLink) {
        String html = """
                <p>Hello,</p>
                <p>Click the button below to verify your email and activate your Escobar.Club account.</p>
                <p><a href="%s">Verify email</a></p>
                <p>If you didn't create this account, you can ignore this email.</p>
                """.formatted(verifyLink);
        send(user.getEmail(), "Verify your Escobar.Club email", html);
    }

    @Override
    public void sendAccountLockedNotice(User user) {
        String html = """
                <p>Hello,</p>
                <p>Your Escobar.Club account was temporarily locked after several failed sign-in attempts.
                If this wasn't you, we recommend resetting your password once the lock expires.</p>
                """;
        send(user.getEmail(), "Your Escobar.Club account was locked", html);
    }

    @Override
    public void sendLoginOtpEmail(User user, String code) {
        String html = """
                <p>Hello,</p>
                <p>Your Escobar.Club sign-in code is:</p>
                <p style="font-size:24px;font-weight:bold;letter-spacing:4px;">%s</p>
                <p>This code expires shortly. If you didn't request this, you can ignore this email.</p>
                """.formatted(code);
        send(user.getEmail(), "Your Escobar.Club sign-in code", html);
    }

    private void send(String to, String subject, String html) {
        try {
            resendRestClient.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + resendProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ResendEmailRequest(emailProperties.from(), List.of(to), subject, html))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Failed to send email to {} via Resend - check RESEND_API_KEY / EMAIL_FROM configuration", to, e);
        }
    }

    private record ResendEmailRequest(String from, List<String> to, String subject, String html) {
    }
}
