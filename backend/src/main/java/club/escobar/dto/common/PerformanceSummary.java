package club.escobar.dto.common;

public record PerformanceSummary(
        PerformanceWindow sevenDay,
        PerformanceWindow thirtyDay,
        PerformanceWindow ninetyDay,
        PerformanceWindow allTime
) {
}
