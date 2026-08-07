package club.escobar.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends ApiException {

    public InvalidOtpException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
