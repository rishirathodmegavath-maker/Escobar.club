package club.escobar.scheduler;

import club.escobar.config.MetricsSyncProperties;
import club.escobar.service.ContentMetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetricsSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(MetricsSyncScheduler.class);

    private final ContentMetricsService contentMetricsService;
    private final MetricsSyncProperties metricsSyncProperties;

    @Scheduled(fixedDelayString = "${app.metrics-sync.scheduler-fixed-delay-ms}")
    public void syncDueMetrics() {
        if (!metricsSyncProperties.schedulerEnabled()) {
            return;
        }
        try {
            contentMetricsService.syncDueMetrics(metricsSyncProperties.schedulerBatchSize());
        } catch (Exception ex) {
            log.error("Scheduled metrics sync run failed", ex);
        }
    }
}
