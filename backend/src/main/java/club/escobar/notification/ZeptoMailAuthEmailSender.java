package club.escobar.notification;

import club.escobar.config.EmailProperties;
import club.escobar.config.ZeptoMailProperties;
import club.escobar.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends real emails via ZeptoMail's HTTP API (https://api.zeptomail.com/v1.1/email).
 * A misconfigured or unreachable provider must never block account creation or password-reset
 * requests, so send failures are caught and logged rather than propagated to the caller.
 */
@Service
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "zeptomail")
@RequiredArgsConstructor
public class ZeptoMailAuthEmailSender implements AuthEmailSender {

    private static final Logger log = LoggerFactory.getLogger(ZeptoMailAuthEmailSender.class);
    private static final Pattern FROM_PATTERN = Pattern.compile("^\\s*(.*?)\\s*<(.+)>\\s*$");

    private final RestClient zeptoMailRestClient;
    private final EmailProperties emailProperties;
    private final ZeptoMailProperties zeptoMailProperties;

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
            zeptoMailRestClient.post()
                    .uri("/v1.1/email")
                    .header("Authorization", "Zoho-enczapikey " + zeptoMailProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ZeptoMailEmailRequest(fromAddress(), List.of(new ZeptoMailRecipient(new ZeptoMailAddress(to, null))), subject, html))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Failed to send email to {} via ZeptoMail - check ZEPTOMAIL_API_KEY / EMAIL_FROM configuration", to, e);
        }
    }

    private ZeptoMailAddress fromAddress() {
        Matcher matcher = FROM_PATTERN.matcher(emailProperties.from());
        if (matcher.matches()) {
            return new ZeptoMailAddress(matcher.group(2), matcher.group(1).isBlank() ? null : matcher.group(1));
        }
        return new ZeptoMailAddress(emailProperties.from(), null);
    }

    private record ZeptoMailEmailRequest(
            ZeptoMailAddress from,
            List<ZeptoMailRecipient> to,
            String subject,
            @JsonProperty("htmlbody") String htmlBody) {
    }

    private record ZeptoMailRecipient(@JsonProperty("email_address") ZeptoMailAddress emailAddress) {
    }

    private record ZeptoMailAddress(String address, String name) {
    }
}
