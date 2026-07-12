package moo.interview.teya.exception;

/**
 * Raised when a transaction request currency does not match the account currency.
 */
public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String message) {
        super(message);
    }
}

