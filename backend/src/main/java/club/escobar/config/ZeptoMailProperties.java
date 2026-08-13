package club.escobar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email.zeptomail")
public record ZeptoMailProperties(String apiKey) {
}
