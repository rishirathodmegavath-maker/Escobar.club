package club.escobar.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over where uploaded media files physically live. The local-disk
 * implementation is used today; a future S3 (or other object store) implementation
 * can be swapped in via a new @Service bean without touching any calling code.
 */
public interface StorageService {

    StoredFile store(MultipartFile file);

    /** Stores a file outside any publicly-served path. Returns an opaque key, not a URL. */
    PrivateStoredFile storePrivate(MultipartFile file);

    /** Resolves a key previously returned by {@link #storePrivate} back to its bytes. */
    StoredFileContent loadPrivate(String key);
}
