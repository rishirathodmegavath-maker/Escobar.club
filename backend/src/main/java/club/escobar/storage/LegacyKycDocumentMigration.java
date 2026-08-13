package club.escobar.storage;

import club.escobar.config.StorageProperties;
import club.escobar.entity.CreatorKycProfile;
import club.escobar.repository.CreatorKycProfileRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * One-time, idempotent cleanup for KYC documents stored before the private-storage migration:
 * their document_url column holds an old publicly-servable path (e.g. "/media/2026-08-01/x.jpg")
 * instead of a private storage key. Runs on every startup but is a no-op once a row has been
 * migrated, since migrated keys never start with "/". Moves (not copies) the file so it stops
 * being reachable at its old public URL.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LegacyKycDocumentMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyKycDocumentMigration.class);

    private final CreatorKycProfileRepository creatorKycProfileRepository;
    private final StorageProperties storageProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<CreatorKycProfile> legacy = creatorKycProfileRepository.findAll().stream()
                .filter(p -> p.getDocumentKey() != null && p.getDocumentKey().startsWith("/"))
                .toList();
        if (legacy.isEmpty()) {
            return;
        }
        log.info("Migrating {} legacy KYC document(s) off the public storage path", legacy.size());
        for (CreatorKycProfile profile : legacy) {
            try {
                migrateOne(profile);
            } catch (IOException e) {
                log.error("Failed to migrate legacy KYC document for creator id={}", profile.getCreator().getId(), e);
            }
        }
    }

    private void migrateOne(CreatorKycProfile profile) throws IOException {
        Long creatorId = profile.getCreator().getId();
        String oldUrl = profile.getDocumentKey();
        String baseUrlPrefix = storageProperties.baseUrl() + "/";
        if (!oldUrl.startsWith(baseUrlPrefix)) {
            log.warn("Legacy KYC document_url for creator id={} doesn't match configured base-url ({}), skipping: {}",
                    creatorId, storageProperties.baseUrl(), oldUrl);
            return;
        }

        String relativeInPublicDir = oldUrl.substring(baseUrlPrefix.length());
        Path source = Path.of(storageProperties.uploadDir(), relativeInPublicDir).normalize();
        if (!Files.exists(source)) {
            log.warn("Legacy KYC document file missing on disk for creator id={}, cannot migrate: {}", creatorId, source);
            return;
        }

        Path targetDir = Path.of(storageProperties.privateUploadDir(), "migrated");
        Files.createDirectories(targetDir);
        String extension = extensionOf(relativeInPublicDir);
        Path target = targetDir.resolve(UUID.randomUUID() + "." + extension);

        Files.move(source, target);

        String newKey = "migrated/" + target.getFileName();
        profile.setDocumentKey(newKey);
        creatorKycProfileRepository.save(profile);
        log.info("Migrated legacy KYC document for creator id={} to private storage", creatorId);
    }

    private String extensionOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : "bin";
    }
}
