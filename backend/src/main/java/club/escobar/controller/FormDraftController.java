package club.escobar.controller;

import club.escobar.dto.drafts.FormDraftResponse;
import club.escobar.dto.drafts.FormDraftSaveRequest;
import club.escobar.security.SecurityUser;
import club.escobar.service.FormDraftService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Auto-save endpoints for in-progress form input, so a user's half-filled form survives an app
 * crash, closed tab, or network drop. Drafts are scoped to the authenticated user and an
 * allowlisted key identifying which form it belongs to; payload is an opaque JSON blob the
 * frontend defines and interprets.
 */
@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
@Validated
public class FormDraftController {

    private static final String DRAFT_KEY_PATTERN = "^[a-z0-9\\-]{1,60}$";

    private final FormDraftService formDraftService;

    @PutMapping("/{key}")
    public ResponseEntity<FormDraftResponse> save(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable("key") @Pattern(regexp = DRAFT_KEY_PATTERN) String key,
            @Valid @RequestBody FormDraftSaveRequest request) {
        return ResponseEntity.ok(formDraftService.save(user.getId(), key, request.payload()));
    }

    @GetMapping("/{key}")
    public ResponseEntity<FormDraftResponse> get(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable("key") @Pattern(regexp = DRAFT_KEY_PATTERN) String key) {
        return formDraftService.get(user.getId(), key)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable("key") @Pattern(regexp = DRAFT_KEY_PATTERN) String key) {
        formDraftService.delete(user.getId(), key);
        return ResponseEntity.noContent().build();
    }
}
