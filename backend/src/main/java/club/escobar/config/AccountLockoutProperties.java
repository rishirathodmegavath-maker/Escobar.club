package club.escobar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.account-lockout")
public record AccountLockoutProperties(int maxAttempts, long durationMinutes) {
}
