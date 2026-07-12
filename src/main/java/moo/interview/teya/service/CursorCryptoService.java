package moo.interview.teya.service;

import org.springframework.stereotype.Service;

/**
 * Simple cursor encoding/decoding service using ID reversal.
 * Example: 12345 -> "54321"
 */
@Service
public class CursorCryptoService {

    public String encode(Long id) {
        if (id == null) return null;
        return new StringBuilder(id.toString()).reverse().toString();
    }

    public Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        String reversed = new StringBuilder(cursor).reverse().toString();
        try {
            return Long.parseLong(reversed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

