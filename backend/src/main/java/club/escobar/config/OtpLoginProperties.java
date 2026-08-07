package club.escobar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.otp-login")
public record OtpLoginProperties(long codeTtlMinutes, int maxAttempts, long minResendIntervalSeconds) {
}
