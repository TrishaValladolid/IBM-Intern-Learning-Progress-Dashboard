package com.dashboard.security;

import com.dashboard.entity.User;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Issues and validates a compact HMAC-SHA256 signed token (a minimal JWT-style token,
 * implemented with JDK crypto only — no JWT library).
 *
 * Format: base64url(payload) + "." + base64url(hmacSha256(payload))
 * where payload = "username|ROLE|expiryEpochSeconds".
 *
 * NOTE: For a production deployment, SIGNING_SECRET must be externalized (e.g. an
 * environment variable or WildFly system property) rather than hardcoded. A constant
 * is acceptable for this capstone; see README for how to override it.
 */
public final class TokenService {

    // Externalize in production. Overridable via -Dauth.signing.secret=... system property.
    private static final String SIGNING_SECRET = System.getProperty(
            "auth.signing.secret",
            "change-me-capstone-dev-secret-please-override-in-production-0xA1B2C3");

    private static final long TOKEN_TTL_SECONDS = 8 * 60 * 60; // 8 hours
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private TokenService() {}

    /** Immutable view of a validated token's claims. */
    public static final class Claims {
        public final String username;
        public final User.Role role;

        Claims(String username, User.Role role) {
            this.username = username;
            this.role = role;
        }
    }

    /** Issues a signed token for the given user, valid for TOKEN_TTL_SECONDS. */
    public static String issue(User user) {
        long expiry = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        String payload = user.getUsername() + "|" + user.getRole().name() + "|" + expiry;
        String encodedPayload = URL_ENCODER.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = sign(encodedPayload);
        return encodedPayload + "." + signature;
    }

    /**
     * Validates signature and expiry. Returns the claims if valid, otherwise null.
     */
    public static Claims validate(String token) {
        if (token == null || token.isBlank()) return null;
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) return null;

        String encodedPayload = token.substring(0, dot);
        String signature = token.substring(dot + 1);

        String expectedSignature = sign(encodedPayload);
        if (!constantTimeEquals(expectedSignature, signature)) return null;

        try {
            String payload = new String(URL_DECODER.decode(encodedPayload), StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|");
            if (parts.length != 3) return null;

            long expiry = Long.parseLong(parts[2]);
            if (Instant.now().getEpochSecond() > expiry) return null; // expired

            User.Role role = User.Role.valueOf(parts[1]);
            return new Claims(parts[0], role);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(SIGNING_SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign token", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ab.length != bb.length) return false;
        int result = 0;
        for (int i = 0; i < ab.length; i++) {
            result |= ab[i] ^ bb[i];
        }
        return result == 0;
    }
}
