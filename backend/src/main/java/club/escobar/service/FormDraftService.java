package club.escobar.service;

import club.escobar.dto.drafts.FormDraftResponse;

import java.util.Optional;

public interface FormDraftService {

    FormDraftResponse save(Long userId, String draftKey, String payload);

    Optional<FormDraftResponse> get(Long userId, String draftKey);

    void delete(Long userId, String draftKey);
}
