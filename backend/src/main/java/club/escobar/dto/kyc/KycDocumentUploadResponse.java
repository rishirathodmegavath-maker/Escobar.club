package club.escobar.dto.kyc;

public record KycDocumentUploadResponse(String documentKey, String contentType, long sizeBytes) {
}
