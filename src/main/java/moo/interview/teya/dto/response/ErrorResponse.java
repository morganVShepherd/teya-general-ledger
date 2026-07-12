package moo.interview.teya.dto.response;

/**
 * Standard error response format for all API error responses.
 * 
 * @param errorCode The error code (e.g., "VALIDATION_ERROR", "NOT_FOUND")
 * @param transactionId Unique identifier for tracking the error
 * @param timestamp ISO-8601 formatted timestamp in UTC
 * @param message Human-readable error message
 */
public record ErrorResponse(
        String errorCode,
        String transactionId,
        String timestamp,
        String message
) {
}

