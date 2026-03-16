package be.vdab.tcoaching;

import be.vdab.tcoaching.api.common.ClientIpAnonymizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientIpAnonymizerTests {
    private final ClientIpAnonymizer anonymizer = new ClientIpAnonymizer();

    @Test
    void anonymizeIpv4TruncatesLastOctet() {
        assertEquals("203.0.113.0", anonymizer.anonymize("203.0.113.42"));
    }

    @Test
    void anonymizeIpv6MasksLowerHalf() {
        assertEquals("2001:db8:0:0:0:0:0:0", anonymizer.anonymize("2001:db8::1234:5678"));
    }

    @Test
    void anonymizeUnparseableValueFallsBackToHash() {
        String value = anonymizer.anonymize("not-a-valid-ip");
        assertTrue(value.startsWith("sha256:"));
    }
}
