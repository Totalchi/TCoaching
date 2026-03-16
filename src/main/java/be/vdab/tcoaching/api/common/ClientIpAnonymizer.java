package be.vdab.tcoaching.api.common;

import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

@Component
public class ClientIpAnonymizer {
    public String anonymize(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }

        String normalized = ipAddress.trim();
        try {
            InetAddress address = InetAddress.getByName(normalized);
            byte[] bytes = address.getAddress().clone();
            if (address instanceof Inet4Address) {
                bytes[3] = 0;
            } else {
                Arrays.fill(bytes, 8, bytes.length, (byte) 0);
            }
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException ex) {
            return hashFallback(normalized);
        }
    }

    private String hashFallback(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash, 0, 12);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
