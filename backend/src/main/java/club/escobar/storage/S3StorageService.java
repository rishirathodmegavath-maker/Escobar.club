package club.escobar.storage;

import club.escobar.config.S3StorageProperties;
import club.escobar.config.StorageProperties;
import club.escobar.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
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
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No file was provided");
        }
        if (file.getSize() > storageProperties.maxFileSizeBytes()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds the maximum allowed size");
        }

        String contentType = file.getContentType();
        List<String> allowed = new ArrayList<>();
        allowed.addAll(storageProperties.allowedImageTypes());
        allowed.addAll(storageProperties.allowedVideoTypes());
        allowed.addAll(storageProperties.allowedDocumentTypes());
        if (contentType == null || allowed.stream().noneMatch(contentType::equalsIgnoreCase)) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported file type: " + contentType);
        }

        String datePath = LocalDate.now().toString();
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String key = datePath + "/" + UUID.randomUUID() + (StringUtils.hasText(extension) ? "." + extension : "");

        try (InputStream inputStream = file.getInputStream()) {
            client().putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Properties.bucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize()));
            log.info("Stored uploaded file at s3://{}/{} ({} bytes, {})", s3Properties.bucket(), key, file.getSize(), contentType);

            String publicUrl = s3Properties.publicBaseUrl() + "/" + key;
            return new StoredFile(publicUrl, contentType, file.getSize());
        } catch (IOException e) {
            log.error("Failed to store uploaded file", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store the uploaded file");
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
