package club.escobar.notification;

import club.escobar.entity.User;

/**
 * Abstraction over how auth-related emails (password reset, email verification) reach the
 * user. The logging implementation is used today; the Resend-backed implementation is a
 * drop-in @Service swap gated on app.email.provider=resend.
 */
public interface AuthEmailSender {

    void sendPasswordResetEmail(User user, String resetLink);

    void sendVerificationEmail(User user, String verifyLink);
}
