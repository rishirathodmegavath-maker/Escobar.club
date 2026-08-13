package club.escobar.service;

import club.escobar.dto.kyc.CreatorKycProfileResponse;
import club.escobar.dto.kyc.CreatorKycReviewDetailResponse;
import club.escobar.dto.kyc.CreatorKycReviewRequest;
import club.escobar.dto.kyc.CreatorKycSubmitRequest;
import club.escobar.entity.CreatorKycProfile;
import club.escobar.entity.User;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.exception.ForbiddenActionException;
import club.escobar.exception.InvalidStateTransitionException;
import club.escobar.mapper.CreatorKycMapper;
import club.escobar.repository.ContentRepository;
import club.escobar.repository.CreatorKycProfileRepository;
import club.escobar.repository.UserRepository;
import club.escobar.service.impl.CreatorKycServiceImpl;
import club.escobar.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreatorKycServiceImplTest {

    @Mock
    private CreatorKycProfileRepository creatorKycProfileRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CreatorKycMapper creatorKycMapper;
    @Mock
    private StorageService storageService;

    @InjectMocks
    private CreatorKycServiceImpl creatorKycService;

    private User creator;

    @BeforeEach
    void setUp() {
        creator = User.builder().id(1L).email("creator@test.com").role(UserRole.CREATOR).active(true).build();
    }

    @Test
    void submit_createsNewProfile_whenNoneExists() {
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(creatorKycProfileRepository.save(any(CreatorKycProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creatorKycMapper.toResponse(any(CreatorKycProfile.class))).thenReturn(mock(CreatorKycProfileResponse.class));

        creatorKycService.submit(1L, new CreatorKycSubmitRequest("ABCDE1234F", "Jamie Rivera", "new-key.jpg"));

        var captor = org.mockito.ArgumentCaptor.forClass(CreatorKycProfile.class);
        verify(creatorKycProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getPanNumber()).isEqualTo("ABCDE1234F");
        assertThat(captor.getValue().getStatus()).isEqualTo(KycStatus.PENDING);
    }

    @Test
    void submit_resetsToPending_whenResubmittingAfterRejection() {
        User reviewer = User.builder().id(2L).email("biz@test.com").role(UserRole.BUSINESS).build();
        CreatorKycProfile existing = CreatorKycProfile.builder().creator(creator).panNumber("OLDPAN123")
                .nameOnPan("Old Name").documentKey("old.jpg").status(KycStatus.REJECTED)
                .reviewedBy(reviewer).reviewNote("Blurry image").reviewedAt(java.time.Instant.now()).build();
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.of(existing));
        when(creatorKycProfileRepository.save(any(CreatorKycProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creatorKycMapper.toResponse(any(CreatorKycProfile.class))).thenReturn(mock(CreatorKycProfileResponse.class));

        creatorKycService.submit(1L, new CreatorKycSubmitRequest("ABCDE1234F", "Jamie Rivera", "new-key-v2.jpg"));

        assertThat(existing.getStatus()).isEqualTo(KycStatus.PENDING);
        assertThat(existing.getReviewedBy()).isNull();
        assertThat(existing.getReviewNote()).isNull();
        assertThat(existing.getPanNumber()).isEqualTo("ABCDE1234F");
    }

    @Test
    void review_rejectsWhenBusinessHasNoRelationshipToCreator() {
        when(contentRepository.existsByCreator_IdAndBusiness_Id(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> creatorKycService.review(2L, 1L, new CreatorKycReviewRequest(KycStatus.VERIFIED, "ok")))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void review_rejectsWhenNotPending() {
        CreatorKycProfile profile = CreatorKycProfile.builder().creator(creator).panNumber("ABCDE1234F")
                .nameOnPan("Jamie").documentKey("doc.jpg").status(KycStatus.VERIFIED).build();
        when(contentRepository.existsByCreator_IdAndBusiness_Id(1L, 2L)).thenReturn(true);
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> creatorKycService.review(2L, 1L, new CreatorKycReviewRequest(KycStatus.REJECTED, "no")))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void review_verifiesPendingProfile_whenRelationshipExists() {
        User reviewer = User.builder().id(2L).email("biz@test.com").role(UserRole.BUSINESS).build();
        CreatorKycProfile profile = CreatorKycProfile.builder().creator(creator).panNumber("ABCDE1234F")
                .nameOnPan("Jamie").documentKey("doc.jpg").status(KycStatus.PENDING).build();
        when(contentRepository.existsByCreator_IdAndBusiness_Id(1L, 2L)).thenReturn(true);
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.of(profile));
        when(userRepository.getReferenceById(2L)).thenReturn(reviewer);
        when(creatorKycProfileRepository.save(any(CreatorKycProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creatorKycMapper.toReviewDetailResponse(any(CreatorKycProfile.class))).thenReturn(mock(CreatorKycReviewDetailResponse.class));

        creatorKycService.review(2L, 1L, new CreatorKycReviewRequest(KycStatus.VERIFIED, "Looks good"));

        assertThat(profile.getStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(profile.getReviewedAt()).isNotNull();
    }

    @Test
    void maskPan_masksFirstSixCharacters() {
        CreatorKycMapper mapper = org.mapstruct.factory.Mappers.getMapper(CreatorKycMapper.class);
        assertThat(mapper.maskPan("ABCDE1234F")).isEqualTo("XXXXXX234F");
    }

    @Test
    void getDocument_allowsCreatorToFetchTheirOwnDocument() {
        CreatorKycProfile profile = CreatorKycProfile.builder().creator(creator).panNumber("ABCDE1234F")
                .nameOnPan("Jamie").documentKey("key.jpg").status(KycStatus.PENDING).build();
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.of(profile));
        var content = new club.escobar.storage.StoredFileContent(new byte[] {1}, "image/jpeg");
        when(storageService.loadPrivate("key.jpg")).thenReturn(content);

        assertThat(creatorKycService.getDocument(1L, "CREATOR", 1L)).isEqualTo(content);
    }

    @Test
    void getDocument_allowsAdminRegardlessOfRelationship() {
        CreatorKycProfile profile = CreatorKycProfile.builder().creator(creator).panNumber("ABCDE1234F")
                .nameOnPan("Jamie").documentKey("key.jpg").status(KycStatus.PENDING).build();
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.of(profile));
        var content = new club.escobar.storage.StoredFileContent(new byte[] {1}, "image/jpeg");
        when(storageService.loadPrivate("key.jpg")).thenReturn(content);

        assertThat(creatorKycService.getDocument(99L, "ADMIN", 1L)).isEqualTo(content);
    }

    @Test
    void getDocument_allowsBusinessOnlyWithExistingRelationship() {
        when(contentRepository.existsByCreator_IdAndBusiness_Id(1L, 2L)).thenReturn(true);
        CreatorKycProfile profile = CreatorKycProfile.builder().creator(creator).panNumber("ABCDE1234F")
                .nameOnPan("Jamie").documentKey("key.jpg").status(KycStatus.PENDING).build();
        when(creatorKycProfileRepository.findByCreator_Id(1L)).thenReturn(Optional.of(profile));
        var content = new club.escobar.storage.StoredFileContent(new byte[] {1}, "image/jpeg");
        when(storageService.loadPrivate("key.jpg")).thenReturn(content);

        assertThat(creatorKycService.getDocument(2L, "BUSINESS", 1L)).isEqualTo(content);
    }

    @Test
    void getDocument_rejectsBusinessWithoutRelationship() {
        when(contentRepository.existsByCreator_IdAndBusiness_Id(1L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> creatorKycService.getDocument(3L, "BUSINESS", 1L))
                .isInstanceOf(ForbiddenActionException.class);
    }

    @Test
    void getDocument_rejectsAnotherCreator() {
        assertThatThrownBy(() -> creatorKycService.getDocument(5L, "CREATOR", 1L))
                .isInstanceOf(ForbiddenActionException.class);
    }
}
