package club.escobar.controller;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.wallet.AddMoneyRequest;
import club.escobar.dto.wallet.WalletSummaryResponse;
import club.escobar.dto.wallet.WalletTransactionResponse;
import club.escobar.entity.enums.WalletTransactionStatus;
import club.escobar.entity.enums.WalletTransactionType;
import club.escobar.security.SecurityUser;
import club.escobar.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/businesses/{id}/wallet")
@PreAuthorize("hasRole('BUSINESS')")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<WalletSummaryResponse> getSummary(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id) {
        return ResponseEntity.ok(walletService.getSummary(user.getId(), id));
    }

    @PostMapping("/transactions")
    public ResponseEntity<WalletTransactionResponse> addMoney(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @Valid @RequestBody AddMoneyRequest request) {
        return ResponseEntity.ok(walletService.addMoney(user.getId(), id, request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<WalletTransactionResponse>> listTransactions(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @RequestParam(required = false) WalletTransactionType type,
            @RequestParam(required = false) WalletTransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(walletService.listTransactions(user.getId(), id, type, status, from, to, pageable));
    }
}
