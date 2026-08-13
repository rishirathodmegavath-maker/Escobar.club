package club.escobar.controller;

import club.escobar.dto.kyc.CreatorKycProfileResponse;
import club.escobar.dto.kyc.CreatorKycReviewDetailResponse;
import club.escobar.dto.kyc.CreatorKycReviewRequest;
import club.escobar.dto.kyc.CreatorKycSubmitRequest;
import club.escobar.dto.kyc.KycDocumentUploadResponse;
import club.escobar.security.SecurityUser;
import club.escobar.service.CreatorKycService;
import club.escobar.storage.PrivateStoredFile;
import club.escobar.storage.StorageService;
import club.escobar.storage.StoredFileContent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class CreatorKycController {

    private final CreatorKycService creatorKycService;
    private final StorageService storageService;

    @PostMapping(value = "/api/kyc/document", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<KycDocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        PrivateStoredFile stored = storageService.storePrivate(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new KycDocumentUploadResponse(stored.key(), stored.contentType(), stored.sizeBytes()));
    }

    @GetMapping("/api/kyc/{creatorId}/document")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getDocument(@AuthenticationPrincipal SecurityUser user, @PathVariable Long creatorId) {
        StoredFileContent content = creatorKycService.getDocument(user.getId(), user.getRole(), creatorId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(content.bytes());
    }

    @PostMapping("/api/kyc/me")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<CreatorKycProfileResponse> submit(
            @AuthenticationPrincipal SecurityUser user,
            @Valid @RequestBody CreatorKycSubmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(creatorKycService.submit(user.getId(), request));
    }

    @GetMapping("/api/kyc/me")
    @PreAuthorize("hasRole('CREATOR')")
    public ResponseEntity<CreatorKycProfileResponse> getOwn(@AuthenticationPrincipal SecurityUser user) {
        return ResponseEntity.ok(creatorKycService.getOwn(user.getId()));
    }

    @GetMapping("/api/creators/{id}/kyc")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<CreatorKycReviewDetailResponse> getForReview(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id) {
        return ResponseEntity.ok(creatorKycService.getForReview(user.getId(), id));
    }

    @PatchMapping("/api/creators/{id}/kyc")
    @PreAuthorize("hasRole('BUSINESS')")
    public ResponseEntity<CreatorKycReviewDetailResponse> review(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable Long id,
            @Valid @RequestBody CreatorKycReviewRequest request) {
        return ResponseEntity.ok(creatorKycService.review(user.getId(), id, request));
    }
}
