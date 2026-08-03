package club.escobar.security;

import club.escobar.config.GoogleOAuthProperties;
import club.escobar.exception.ApiException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Collections;

/** Verifies a Google ID token obtained client-side (via @react-oauth/google) against Google's public keys. */
@Service
@RequiredArgsConstructor
public class GoogleIdTokenVerifierService {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifierService.class);

    private final GoogleOAuthProperties properties;

    private volatile GoogleIdTokenVerifier verifier;

    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier().verify(idTokenString);
            if (idToken == null) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Could not verify Google sign-in");
            }
            return idToken.getPayload();
        } catch (ApiException e) {
            throw e;
        } catch (GeneralSecurityException | java.io.IOException | IllegalArgumentException e) {
            log.warn("Google ID token verification failed", e);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Could not verify Google sign-in");
        }
    }

    private GoogleIdTokenVerifier verifier() throws GeneralSecurityException, java.io.IOException {
        GoogleIdTokenVerifier v = verifier;
        if (v == null) {
            synchronized (this) {
                v = verifier;
                if (v == null) {
                    v = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                            .setAudience(Collections.singletonList(properties.clientId()))
                            .build();
                    verifier = v;
                }
            }
        }
        return v;
    }
}
