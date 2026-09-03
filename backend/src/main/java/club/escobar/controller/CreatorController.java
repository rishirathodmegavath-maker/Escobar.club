package club.escobar.controller;

import club.escobar.dto.creator.CreatorDashboardResponse;
import club.escobar.dto.creator.CreatorProfileResponse;
import club.escobar.dto.creator.CreatorProfileUpdateRequest;
import club.escobar.security.SecurityUser;
import club.escobar.service.CreatorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorProfileService creatorProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<CreatorProfileResponse> getOwnProfile(@AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(creatorProfileService.getByUserId(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreatorProfileResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(creatorProfileService.getByUserId(id));
    }

    @GetMapping("/me/dashboard")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<CreatorDashboardResponse> dashboard(@AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(creatorProfileService.dashboard(user.getId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<CreatorProfileResponse> updateOwnProfile(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody CreatorProfileUpdateRequest request) {
        return ResponseEntity.ok(creatorProfileService.updateOwnProfile(user.getId(), request));
    }
}
