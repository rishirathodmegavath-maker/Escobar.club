package club.escobar.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends ApiException {

    public AccountLockedException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
