package club.escobar.notification;

import club.escobar.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Used in local dev. Set {@code app.email.provider=resend} to send real emails instead. */
@Service
@ConditionalOnProperty(prefix = "app.email", name = "provider", havingValue = "logging", matchIfMissing = true)
public class LoggingAuthEmailSender implements AuthEmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuthEmailSender.class);

    @Override
    public void sendPasswordResetEmail(User user, String resetLink) {
        log.info("Password reset requested for user id={} email={}. Reset link: {}", user.getId(), user.getEmail(), resetLink);
    }

    @Override
    public void sendVerificationEmail(User user, String verifyLink) {
        log.info("Verification email requested for user id={} email={}. Verify link: {}", user.getId(), user.getEmail(), verifyLink);
    }

    @Override
    public void sendAccountLockedNotice(User user) {
        log.info("Account locked due to repeated failed login attempts: user id={} email={}", user.getId(), user.getEmail());
    }

    @Override
    public void sendLoginOtpEmail(User user, String code) {
        log.info("Login OTP requested for user id={} email={}. Code: {}", user.getId(), user.getEmail(), code);
    }
}
