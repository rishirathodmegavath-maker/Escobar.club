package club.escobar.dto.common;

public record NeedsAttentionItem(
        String message,
        String actionLabel,
        String actionPath
) {
}
