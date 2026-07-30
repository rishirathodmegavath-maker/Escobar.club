package club.escobar.controller;

import club.escobar.dto.campaign.CampaignCreateRequest;
import club.escobar.dto.campaign.CampaignResponse;
import club.escobar.dto.campaign.CampaignUpdateRequest;
import club.escobar.dto.common.PageResponse;
import club.escobar.security.SecurityUser;
import club.escobar.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping("/api/campaigns")
    public ResponseEntity<PageResponse<CampaignResponse>> browse(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(campaignService.listPublic(search, category, pageable));
    }

    @GetMapping("/api/campaigns/{id}")
    public ResponseEntity<CampaignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getById(id));
    }

    @GetMapping("/api/campaigns/mine")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<PageResponse<CampaignResponse>> mine(
            @AuthenticationPrincipal SecurityUser user,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(campaignService.listMine(user.getId(), pageable));
    }

    @PostMapping("/api/campaigns")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<CampaignResponse> create(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody CampaignCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.create(user.getId(), request));
    }

    @PutMapping("/api/campaigns/{id}")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<CampaignResponse> update(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @Valid @RequestBody CampaignUpdateRequest request) {
        return ResponseEntity.ok(campaignService.update(user.getId(), id, request));
    }
}
