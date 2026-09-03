package club.escobar.dto.common;

public record PerformanceWindow(
        long views,
        long likes,
        long comments,
        long publishedCount,
        // (likes + comments) / views * 100, rounded to 1 decimal. Zero when views is zero, never a
        // divide-by-zero. Never a fabricated week-over-week trend - just the ratio as it stands today.
        double engagementRate
) {
}
