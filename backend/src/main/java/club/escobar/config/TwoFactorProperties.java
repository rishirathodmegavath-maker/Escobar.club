package club.escobar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.two-factor")
public record TwoFactorProperties(String issuer, String secretEncryptionKey) {
}
