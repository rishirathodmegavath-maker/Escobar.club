package club.escobar.storage;

import club.escobar.config.S3StorageProperties;
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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stores uploads in any S3-compatible object store (Hostinger Object Storage, Cloudflare R2,
 * MinIO, etc.) via a configurable endpoint. Activated via {@code app.storage.provider=s3}.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "s3")
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final StorageProperties storageProperties;
    private final S3StorageProperties s3Properties;

    @Override
    public StoredFile store(MultipartFile file) {
        ValidatedUpload validated = validate(file);
        String datePath = LocalDate.now().toString();
        String key = datePath + "/" + UUID.randomUUID() + "." + validated.verified().extension();

        try {
            putObject(key, validated);
            log.info("Stored uploaded file at s3://{}/{} ({} bytes, {})", s3Properties.bucket(), key, validated.bytes().length, validated.verified().contentType());

            String publicUrl = s3Properties.publicBaseUrl() + "/" + key;
            return new StoredFile(publicUrl, validated.verified().contentType(), (long) validated.bytes().length);
        } catch (SdkException e) {
            log.error("Failed to store uploaded file", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store the uploaded file");
        }
    }

    @Override
    public PrivateStoredFile storePrivate(MultipartFile file) {
        ValidatedUpload validated = validate(file);
        String datePath = LocalDate.now().toString();
        // "private/" prefix is a naming convention only - the bucket/CDN in front of it must itself
        // deny public read on this prefix (or use a separate private bucket) for this to be effective.
        String key = "private/kyc/" + datePath + "/" + UUID.randomUUID() + "." + validated.verified().extension();

        try {
            putObject(key, validated);
            log.info("Stored private file at s3://{}/{} ({} bytes, {})", s3Properties.bucket(), key, validated.bytes().length, validated.verified().contentType());
            return new PrivateStoredFile(key, validated.verified().contentType(), (long) validated.bytes().length);
        } catch (SdkException e) {
            log.error("Failed to store private file", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store the uploaded file");
        }
    }

    @Override
    public StoredFileContent loadPrivate(String key) {
        if (key == null || key.isBlank()) {
            throw new ResourceNotFoundException("No document has been uploaded");
        }
        try {
            byte[] bytes = client()
                    .getObjectAsBytes(GetObjectRequest.builder().bucket(s3Properties.bucket()).key(key).build())
                    .asByteArray();
            String contentType = UploadValidator.verify(bytes).contentType();
            return new StoredFileContent(bytes, contentType);
        } catch (NoSuchKeyException e) {
            throw new ResourceNotFoundException("Document not found");
        } catch (SdkException e) {
            log.error("Failed to load private file s3://{}/{}", s3Properties.bucket(), key, e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load the document");
        }
    }

    private void putObject(String key, ValidatedUpload validated) {
        client().putObject(
                PutObjectRequest.builder()
                        .bucket(s3Properties.bucket())
                        .key(key)
                        .contentType(validated.verified().contentType())
                        .build(),
                RequestBody.fromInputStream(new ByteArrayInputStream(validated.bytes()), validated.bytes().length));
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

    private S3Client client() {
        return S3Client.builder()
                .endpointOverride(URI.create(s3Properties.endpoint()))
                .region(Region.of(s3Properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(s3Properties.accessKey(), s3Properties.secretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
