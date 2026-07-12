package moo.interview.teya.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CursorCryptoServiceTest {

    private final CursorCryptoService service = new CursorCryptoService();

    @Test
    void encodeDecode_roundTrip() {
        Long id = 12345L;
        String cursor = service.encode(id);
        assertNotNull(cursor);
        assertEquals("54321", cursor);

        Long decoded = service.decode(cursor);
        assertEquals(id, decoded);
    }

    @Test
    void encodeNull_returnsNull() {
        assertNull(service.encode(null));
    }

    @Test
    void decodeInvalid_returnsNull() {
        assertNull(service.decode("not-a-number"));
    }
}

