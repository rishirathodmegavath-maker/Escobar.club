package club.escobar.storage;

import club.escobar.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Verifies uploaded file content against its magic bytes rather than trusting the client-supplied
 * Content-Type header, and maps the verified type to a fixed, safe extension - the stored filename's
 * extension is never derived from client input (header or original filename), which is what previously
 * allowed an HTML/SVG file to be uploaded as a fake "image/png" and served back as text/html.
 */
final class UploadValidator {

    private UploadValidator() {
    }

    record Verified(String contentType, String extension) {
    }

    static Verified verify(byte[] bytes) {
        if (matches(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return new Verified("image/png", "png");
        }
        if (matches(bytes, 0xFF, 0xD8, 0xFF)) {
            return new Verified("image/jpeg", "jpg");
        }
        if (matchesAscii(bytes, 0, "GIF8")) {
            return new Verified("image/gif", "gif");
        }
        if (matchesAscii(bytes, 0, "RIFF") && matchesAscii(bytes, 8, "WEBP")) {
            return new Verified("image/webp", "webp");
        }
        if (matches(bytes, 0x25, 0x50, 0x44, 0x46)) {
            return new Verified("application/pdf", "pdf");
        }
        if (matchesAscii(bytes, 4, "ftyp")) {
            // ISO base media container - covers both video/mp4 and video/quicktime uploads;
            // the ftyp brand alone isn't a reliable way to tell them apart, and mp4 is the
            // more broadly browser-compatible of the two, so both are normalized to it.
            return new Verified("video/mp4", "mp4");
        }
        if (matches(bytes, 0x1A, 0x45, 0xDF, 0xA3)) {
            return new Verified("video/webm", "webm");
        }
        throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Could not verify file content as a supported image, video, or PDF");
    }

    private static boolean matches(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesAscii(byte[] bytes, int offset, String ascii) {
        if (bytes.length < offset + ascii.length()) {
            return false;
        }
        for (int i = 0; i < ascii.length(); i++) {
            if (bytes[offset + i] != (byte) ascii.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
