package club.escobar.controller;

import club.escobar.dto.common.PageResponse;
import club.escobar.dto.content.ContentCreateRequest;
import club.escobar.dto.content.ContentPublishRequest;
import club.escobar.dto.content.ContentResponse;
import club.escobar.dto.content.ContentReviewRequest;
import club.escobar.dto.content.ContentUpdateRequest;
import club.escobar.entity.enums.ContentStatus;
import club.escobar.security.SecurityUser;
import club.escobar.service.ContentService;
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
public class ContentController {

    private final ContentService contentService;

    @PostMapping("/api/campaigns/{id}/content")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<ContentResponse> submit(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable("id") Long campaignId,
            @Valid @RequestBody ContentCreateRequest request) {
        ContentCreateRequest scoped = new ContentCreateRequest(campaignId, request.caption(), request.mediaUrl(), request.mediaType());
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.submit(user.getId(), scoped));
    }

    @PatchMapping("/api/content/{id}")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<ContentResponse> resubmit(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @Valid @RequestBody ContentUpdateRequest request) {
        return ResponseEntity.ok(contentService.resubmit(user.getId(), id, request));
    }

    @PatchMapping("/api/content/{id}/review")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<ContentResponse> review(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @Valid @RequestBody ContentReviewRequest request) {
        return ResponseEntity.ok(contentService.review(user.getId(), id, request));
    }

    @PatchMapping("/api/content/{id}/publish")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<ContentResponse> publish(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @Valid @RequestBody ContentPublishRequest request) {
        return ResponseEntity.ok(contentService.publish(user.getId(), id, request));
    }

    @GetMapping("/api/businesses/{id}/content")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<PageResponse<ContentResponse>> reviewQueue(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @RequestParam(required = false) ContentStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(contentService.listForBusiness(user.getId(), id, status, pageable));
    }

    @GetMapping("/api/content/me")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<PageResponse<ContentResponse>> myContent(
            @AuthenticationPrincipal SecurityUser user,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(contentService.listForCreator(user.getId(), pageable));
    }

    @GetMapping("/api/content/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contentService.getById(id));
    }
}
