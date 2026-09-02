package club.escobar.controller;

import club.escobar.dto.business.BusinessDashboardResponse;
import club.escobar.dto.business.BusinessProfileResponse;
import club.escobar.dto.business.BusinessProfileUpdateRequest;
import club.escobar.dto.common.PageResponse;
import club.escobar.security.SecurityUser;
import club.escobar.service.BusinessProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessProfileService businessProfileService;

    @GetMapping("/api/businesses")
    public ResponseEntity<PageResponse<BusinessProfileResponse>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String industry,
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(businessProfileService.search(search, industry, pageable));
    }

    @GetMapping("/api/businesses/{id}")
    public ResponseEntity<BusinessProfileResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(businessProfileService.getById(id));
    }

    @GetMapping("/api/businesses/me")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<BusinessProfileResponse> getOwnProfile(@AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(businessProfileService.getByUserId(user.getId()));
    }

    @PutMapping("/api/businesses/me")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<BusinessProfileResponse> updateOwnProfile(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody BusinessProfileUpdateRequest request) {
        return ResponseEntity.ok(businessProfileService.updateOwnProfile(user.getId(), request));
    }

    @GetMapping("/api/businesses/me/dashboard")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<BusinessDashboardResponse> getOwnDashboard(@AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(businessProfileService.dashboard(user.getId()));
    }
}
