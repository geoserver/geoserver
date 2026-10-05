/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Builds the keys of the validation caches: the validation context (endpoint or key set URL, plus any setting that
 * changes the outcome) followed by a SHA-256 digest of the token, so a token accepted in one context is never taken as
 * accepted in another, and the caches do not hold the tokens themselves.
 */
final class TokenCacheKey {

    private TokenCacheKey() {}

    static String of(String context, String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return context + "\n" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
