package club.escobar.storage;

import club.escobar.config.StorageProperties;
import club.escobar.entity.CreatorKycProfile;
import club.escobar.entity.User;
import club.escobar.entity.enums.KycStatus;
import club.escobar.entity.enums.UserRole;
import club.escobar.repository.CreatorKycProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LegacyKycDocumentMigrationTest {

    @Mock
    private CreatorKycProfileRepository creatorKycProfileRepository;

    @TempDir
    Path tempDir;

    private StorageProperties properties(Path uploadDir, Path privateUploadDir) {
        return new StorageProperties("local", uploadDir.toString(), "/media", privateUploadDir.toString(),
                20_971_520L, List.of("image/png"), List.of("video/mp4"), List.of("application/pdf"));
    }

    private CreatorKycProfile profileWith(String documentKey) {
        User creator = User.builder().id(1L).email("creator@test.com").role(UserRole.CREATOR).active(true).build();
        return CreatorKycProfile.builder().creator(creator).panNumber("ABCDE1234F").nameOnPan("Jamie")
                .documentKey(documentKey).status(KycStatus.VERIFIED).build();
    }

    @Test
    void migratesLegacyPublicPathIntoPrivateStorage() throws IOException {
        Path uploadDir = tempDir.resolve("uploads");
        Path privateDir = tempDir.resolve("uploads-private");
        Files.createDirectories(uploadDir.resolve("2026-08-01"));
        Path oldFile = uploadDir.resolve("2026-08-01/abc.jpg");
        Files.writeString(oldFile, "fake-image-bytes");

        CreatorKycProfile profile = profileWith("/media/2026-08-01/abc.jpg");
        when(creatorKycProfileRepository.findAll()).thenReturn(List.of(profile));

        new LegacyKycDocumentMigration(creatorKycProfileRepository, properties(uploadDir, privateDir))
                .run(null);

        assertThat(Files.exists(oldFile)).isFalse();
        assertThat(profile.getDocumentKey()).startsWith("migrated/");
        assertThat(profile.getDocumentKey()).doesNotStartWith("/");
        Path movedFile = privateDir.resolve(profile.getDocumentKey());
        assertThat(Files.exists(movedFile)).isTrue();
        assertThat(Files.readString(movedFile)).isEqualTo("fake-image-bytes");
        verify(creatorKycProfileRepository).save(profile);
    }

    @Test
    void skipsProfilesAlreadyOnPrivateKeys() {
        CreatorKycProfile profile = profileWith("2026-08-01/already-migrated.jpg");
        when(creatorKycProfileRepository.findAll()).thenReturn(List.of(profile));

        new LegacyKycDocumentMigration(creatorKycProfileRepository, properties(tempDir.resolve("uploads"), tempDir.resolve("uploads-private")))
                .run(null);

        verify(creatorKycProfileRepository, never()).save(any());
        assertThat(profile.getDocumentKey()).isEqualTo("2026-08-01/already-migrated.jpg");
    }

    @Test
    void leavesProfileUntouchedWhenSourceFileIsMissing() {
        CreatorKycProfile profile = profileWith("/media/2026-08-01/missing.jpg");
        when(creatorKycProfileRepository.findAll()).thenReturn(List.of(profile));

        new LegacyKycDocumentMigration(creatorKycProfileRepository, properties(tempDir.resolve("uploads"), tempDir.resolve("uploads-private")))
                .run(null);

        verify(creatorKycProfileRepository, never()).save(any());
        assertThat(profile.getDocumentKey()).isEqualTo("/media/2026-08-01/missing.jpg");
    }
}
