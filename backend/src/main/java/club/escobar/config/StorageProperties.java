package club.escobar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String provider,
        String uploadDir,
        String baseUrl,
        // Never registered with any ResourceHandler/nginx location - files here are only reachable
        // through an authenticated controller method, unlike uploadDir which is served at baseUrl/**.
        String privateUploadDir,
        long maxFileSizeBytes,
        List<String> allowedImageTypes,
        List<String> allowedVideoTypes,
        List<String> allowedDocumentTypes
) {
}
