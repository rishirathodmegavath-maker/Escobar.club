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
import club.escobar.entity.enums.ApprovalStatus;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.entity.enums.KycStatus;
import club.escobar.security.SecurityUser;
import club.escobar.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

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
}
