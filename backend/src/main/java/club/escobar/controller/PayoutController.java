package club.escobar.controller;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.payout.PayoutMarkPaidRequest;
import club.escobar.dto.payout.PayoutResponse;
import club.escobar.entity.enums.PayoutStatus;
import club.escobar.security.SecurityUser;
import club.escobar.service.PayoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PayoutController {

    private final PayoutService payoutService;

    @GetMapping("/api/content/{id}/payout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PayoutResponse> getForContent(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id) {
        return ResponseEntity.ok(payoutService.getForContent(user.getId(), id));
    }

    @GetMapping("/api/businesses/{id}/payouts")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<PageResponse<PayoutResponse>> listForBusiness(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @RequestParam(required = false) PayoutStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(payoutService.listForBusiness(user.getId(), id, status, pageable));
    }

    @GetMapping("/api/businesses/{id}/payouts/export")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<byte[]> exportPayoutsCsv(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @RequestParam(required = false) PayoutStatus status) {
        List<PayoutResponse> payouts = payoutService.listForBusinessAll(user.getId(), id, status);
        byte[] csv = toCsv(payouts);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("payouts.csv", StandardCharsets.UTF_8).build().toString())
                .body(csv);
    }

    private static byte[] toCsv(List<PayoutResponse> payouts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8)) {
            writer.println("Creator,Campaign,Views Used,Amount (INR),Status,Paid At");
            for (PayoutResponse p : payouts) {
                writer.println(String.join(",",
                        csvField(p.creatorDisplayName()),
                        csvField(p.campaignTitle()),
                        csvField(p.viewCountUsed() == null ? "" : String.valueOf(p.viewCountUsed())),
                        csvField(p.amountInr() == null ? "" : p.amountInr().toPlainString()),
                        csvField(p.status() == null ? "" : p.status().name()),
                        csvField(p.paidAt() == null ? "" : DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(p.paidAt()))));
            }
        }
        return out.toByteArray();
    }

    // Quotes every field and escapes embedded quotes for correct CSV encoding. Also guards against
    // formula injection: a cell value starting with =/+/-/@ is executed as a formula by Excel/Sheets
    // regardless of CSV quoting, so a leading apostrophe is inserted to force it to be read as text -
    // relevant here because creator display names and campaign titles are user-supplied.
    private static String csvField(String value) {
        String safe = value.matches("^[=+\\-@\\t\\r].*") ? "'" + value : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    @PatchMapping("/api/content/{id}/payout/paid")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<PayoutResponse> markPaid(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @Valid @RequestBody PayoutMarkPaidRequest request) {
        return ResponseEntity.ok(payoutService.markPaid(user.getId(), id, request));
    }
}
