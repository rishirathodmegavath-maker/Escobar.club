package club.escobar.controller;

import club.escobar.dto.admin.AdminBusinessSummaryResponse;
import club.escobar.dto.admin.AdminCampaignSummaryResponse;
import club.escobar.dto.admin.AdminContentSummaryResponse;
import club.escobar.dto.admin.AdminCreatorSummaryResponse;
import club.escobar.dto.admin.AdminDashboardResponse;
import club.escobar.dto.admin.ApprovalDecisionRequest;
import club.escobar.dto.admin.CampaignDisplayStatusUpdateRequest;
import club.escobar.dto.admin.CreatorStatusUpdateRequest;
import club.escobar.dto.common.PageResponse;
import club.escobar.dto.kyc.CreatorKycReviewDetailResponse;
import club.escobar.dto.kyc.CreatorKycReviewRequest;
import club.escobar.dto.wallet.AdminCreditRequest;
import club.escobar.dto.wallet.WalletReversalRequest;
import club.escobar.dto.wallet.WalletReviewRequest;
import club.escobar.dto.wallet.WalletSummaryResponse;
import club.escobar.dto.wallet.WalletTransactionResponse;
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.FundingSource;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.WalletTransactionStatus;
import club.escobar.entity.enums.WalletTransactionType;
import club.escobar.security.SecurityUser;
import club.escobar.service.AdminService;
import club.escobar.service.WalletService;
import club.escobar.util.CsvUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final WalletService walletService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        return ResponseEntity.ok(adminService.dashboard());
    }

    @GetMapping("/creators")
    public ResponseEntity<PageResponse<AdminCreatorSummaryResponse>> listCreators(
            @RequestParam(required = false) KycStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listCreators(status, pageable));
    }

    @PatchMapping("/creators/{userId}/kyc")
    public ResponseEntity<CreatorKycReviewDetailResponse> reviewCreatorKyc(
            @AuthenticationPrincipal SecurityUser admin,
            @PathVariable Long userId,
            @Valid @RequestBody CreatorKycReviewRequest request) {
        return ResponseEntity.ok(adminService.reviewCreatorKyc(admin.getId(), userId, request));
    }

    @PatchMapping("/creators/{userId}/status")
    public ResponseEntity<AdminCreatorSummaryResponse> setCreatorStatus(
            @PathVariable Long userId,
            @Valid @RequestBody CreatorStatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.setCreatorActive(userId, request.active()));
    }

    @GetMapping("/businesses")
    public ResponseEntity<PageResponse<AdminBusinessSummaryResponse>> listBusinesses(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listBusinesses(status, search, pageable));
    }

    @PatchMapping("/businesses/{userId}/approval")
    public ResponseEntity<AdminBusinessSummaryResponse> reviewBusiness(
            @PathVariable Long userId,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        return ResponseEntity.ok(adminService.reviewBusiness(userId, request));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<PageResponse<AdminCampaignSummaryResponse>> listCampaigns(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listCampaigns(status, search, pageable));
    }

    @PatchMapping("/campaigns/{id}/approval")
    public ResponseEntity<AdminCampaignSummaryResponse> reviewCampaign(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        return ResponseEntity.ok(adminService.reviewCampaign(id, request));
    }

    @PatchMapping("/campaigns/{id}/display-status")
    public ResponseEntity<AdminCampaignSummaryResponse> setCampaignDisplayStatus(
            @PathVariable Long id,
            @Valid @RequestBody CampaignDisplayStatusUpdateRequest request) {
        return ResponseEntity.ok(adminService.setCampaignDisplayStatus(id, request));
    }

    @GetMapping("/content")
    public ResponseEntity<PageResponse<AdminContentSummaryResponse>> listContent(
            @RequestParam(required = false) ContentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.listContent(status, pageable));
    }

    @PatchMapping("/content/{id}/link-review")
    public ResponseEntity<AdminContentSummaryResponse> reviewContentLink(
            @AuthenticationPrincipal SecurityUser admin,
            @PathVariable Long id,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        return ResponseEntity.ok(adminService.reviewContentLink(admin.getId(), id, request));
    }

    @GetMapping("/wallets")
    public ResponseEntity<PageResponse<WalletSummaryResponse>> listWallets(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(walletService.adminListWallets(search, pageable));
    }

    @GetMapping("/wallets/{businessId}")
    public ResponseEntity<WalletSummaryResponse> getWallet(@PathVariable Long businessId) {
        return ResponseEntity.ok(walletService.adminGetWallet(businessId));
    }

    @PostMapping("/wallets/{businessId}/credit")
    public ResponseEntity<WalletTransactionResponse> creditWallet(
            @AuthenticationPrincipal SecurityUser admin,
            @PathVariable Long businessId,
            @Valid @RequestBody AdminCreditRequest request) {
        return ResponseEntity.ok(walletService.adminCredit(admin.getId(), businessId, request));
    }

    @GetMapping("/wallet-transactions")
    public ResponseEntity<PageResponse<WalletTransactionResponse>> listWalletTransactions(
            @RequestParam(required = false) Long businessId,
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(required = false) WalletTransactionStatus status,
            @RequestParam(required = false) FundingSource fundingSource,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(walletService.adminListTransactions(businessId, type, status, fundingSource, from, to, search, pageable));
    }

    @GetMapping("/wallet-transactions/export")
    public ResponseEntity<byte[]> exportWalletTransactionsCsv(
            @RequestParam(required = false) Long businessId,
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(required = false) WalletTransactionStatus status,
            @RequestParam(required = false) FundingSource fundingSource,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String search) {
        List<WalletTransactionResponse> transactions =
                walletService.adminListTransactionsAll(businessId, type, status, fundingSource, from, to, search);
        byte[] csv = toWalletCsv(transactions);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("wallet-transactions.csv", StandardCharsets.UTF_8).build().toString())
                .body(csv);
    }

    private static byte[] toWalletCsv(List<WalletTransactionResponse> transactions) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8)) {
            writer.println("Business,Type,Status,Funding Source,Amount (INR),Note,Performed By,Created At");
            for (WalletTransactionResponse t : transactions) {
                writer.println(String.join(",",
                        CsvUtil.csvField(t.businessName()),
                        CsvUtil.csvField(t.type() == null ? "" : t.type().name()),
                        CsvUtil.csvField(t.status() == null ? "" : t.status().name()),
                        CsvUtil.csvField(t.fundingSource() == null ? "" : t.fundingSource().name()),
                        CsvUtil.csvField(t.amountInr() == null ? "" : t.amountInr().toPlainString()),
                        CsvUtil.csvField(t.note()),
                        CsvUtil.csvField(t.performedByName()),
                        CsvUtil.csvField(t.createdAt() == null ? "" : DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(t.createdAt()))));
            }
        }
        return out.toByteArray();
    }

    @PatchMapping("/wallet-transactions/{id}/review")
    public ResponseEntity<WalletTransactionResponse> reviewWalletTopUp(
            @AuthenticationPrincipal SecurityUser admin,
            @PathVariable Long id,
            @Valid @RequestBody WalletReviewRequest request) {
        return ResponseEntity.ok(walletService.reviewTopUp(admin.getId(), id, request));
    }

    @PatchMapping("/wallet-transactions/{id}/reverse")
    public ResponseEntity<WalletTransactionResponse> reverseWalletTransaction(
            @AuthenticationPrincipal SecurityUser admin,
            @PathVariable Long id,
            @Valid @RequestBody WalletReversalRequest request) {
        return ResponseEntity.ok(walletService.reverseTransaction(admin.getId(), id, request));
    }
}
