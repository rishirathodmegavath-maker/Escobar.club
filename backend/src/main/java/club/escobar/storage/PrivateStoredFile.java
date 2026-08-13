package club.escobar.storage;

/** {@code key} is an internal storage reference, never a fetchable URL - it must only ever be
 * resolved back to bytes through an authenticated controller method (see {@code loadPrivate}). */
public record PrivateStoredFile(String key, String contentType, long sizeBytes) {
}
