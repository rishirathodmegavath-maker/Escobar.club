package club.escobar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.metrics-sync")
public record MetricsSyncProperties(
        long minIntervalMinutes,
        boolean schedulerEnabled,
        long schedulerFixedDelayMs,
        int schedulerBatchSize
) {
}
