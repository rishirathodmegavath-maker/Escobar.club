package club.escobar.service.impl;

import club.escobar.dto.drafts.FormDraftResponse;
import club.escobar.entity.FormDraft;
import club.escobar.repository.FormDraftRepository;
import club.escobar.repository.UserRepository;
import club.escobar.service.FormDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FormDraftServiceImpl implements FormDraftService {

    private final FormDraftRepository formDraftRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FormDraftResponse save(Long userId, String draftKey, String payload) {
        FormDraft draft = formDraftRepository.findByUser_IdAndDraftKey(userId, draftKey)
                .orElseGet(() -> FormDraft.builder()
                        .user(userRepository.getReferenceById(userId))
                        .draftKey(draftKey)
                        .build());

        draft.setPayload(payload);
        FormDraft saved = formDraftRepository.save(draft);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDraftResponse> get(Long userId, String draftKey) {
        return formDraftRepository.findByUser_IdAndDraftKey(userId, draftKey).map(this::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long userId, String draftKey) {
        formDraftRepository.deleteByUser_IdAndDraftKey(userId, draftKey);
    }

    private FormDraftResponse toResponse(FormDraft draft) {
        return new FormDraftResponse(draft.getDraftKey(), draft.getPayload(), draft.getUpdatedAt());
    }
}
