package club.escobar.repository;

import club.escobar.entity.FormDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FormDraftRepository extends JpaRepository<FormDraft, Long> {

    Optional<FormDraft> findByUser_IdAndDraftKey(Long userId, String draftKey);

    void deleteByUser_IdAndDraftKey(Long userId, String draftKey);
}
