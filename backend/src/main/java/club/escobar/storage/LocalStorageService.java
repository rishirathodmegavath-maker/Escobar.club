package club.escobar.storage;

import club.escobar.config.StorageProperties;
import club.escobar.exception.ApiException;
import club.escobar.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Used in local dev. Production points {@code app.storage.provider} at {@code s3} instead (see {@link S3StorageService}). */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final StorageProperties storageProperties;

    @Override
    public StoredFile store(MultipartFile file) {
        ValidatedUpload validated = validate(file);
        try {
            String datePath = LocalDate.now().toString();
            Path targetDir = Path.of(storageProperties.uploadDir(), datePath);
            Files.createDirectories(targetDir);

            String filename = UUID.randomUUID() + "." + validated.verified().extension();
            Path targetPath = targetDir.resolve(filename);

            Files.write(targetPath, validated.bytes());
            log.info("Stored uploaded file at {} ({} bytes, {})", targetPath, validated.bytes().length, validated.verified().contentType());

            String publicUrl = storageProperties.baseUrl() + "/" + datePath + "/" + filename;
            return new StoredFile(publicUrl, validated.verified().contentType(), (long) validated.bytes().length);
        } catch (IOException e) {
            log.error("Failed to store uploaded file", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store the uploaded file");
        }
    }

    @Override
    public PrivateStoredFile storePrivate(MultipartFile file) {
        ValidatedUpload validated = validate(file);
        try {
            String datePath = LocalDate.now().toString();
            Path targetDir = Path.of(storageProperties.privateUploadDir(), datePath);
            Files.createDirectories(targetDir);

            String filename = UUID.randomUUID() + "." + validated.verified().extension();
            Path targetPath = targetDir.resolve(filename);

            Files.write(targetPath, validated.bytes());
            log.info("Stored private file at {} ({} bytes, {})", targetPath, validated.bytes().length, validated.verified().contentType());

            String key = datePath + "/" + filename;
            return new PrivateStoredFile(key, validated.verified().contentType(), (long) validated.bytes().length);
        } catch (IOException e) {
            log.error("Failed to store private file", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store the uploaded file");
        }
    }

    @Override
    public StoredFileContent loadPrivate(String key) {
        if (key == null || key.isBlank()) {
            throw new ResourceNotFoundException("No document has been uploaded");
        }
        Path base = Path.of(storageProperties.privateUploadDir()).toAbsolutePath().normalize();
        Path target = base.resolve(key).normalize();
        if (!target.startsWith(base)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid document reference");
        }
        if (!Files.exists(target)) {
            throw new ResourceNotFoundException("Document not found");
        }
        try {
            byte[] bytes = Files.readAllBytes(target);
            String contentType = UploadValidator.verify(bytes).contentType();
            return new StoredFileContent(bytes, contentType);
        } catch (IOException e) {
            log.error("Failed to read private file {}", target, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load the document");
        }
    }

    private record ValidatedUpload(byte[] bytes, UploadValidator.Verified verified) {
    }

    private ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No file was provided");
        }
        if (file.getSize() > storageProperties.maxFileSizeBytes()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the maximum allowed size");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read uploaded file", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store the uploaded file");
        }

        UploadValidator.Verified verified = UploadValidator.verify(bytes);
        assertAllowed(verified.contentType());
        return new ValidatedUpload(bytes, verified);
    }

    private void assertAllowed(String contentType) {
        List<String> allowed = new ArrayList<>();
        allowed.addAll(storageProperties.allowedImageTypes());
        allowed.addAll(storageProperties.allowedVideoTypes());
        allowed.addAll(storageProperties.allowedDocumentTypes());
        if (allowed.stream().noneMatch(contentType::equalsIgnoreCase)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type: " + contentType);
        }
    }
}
