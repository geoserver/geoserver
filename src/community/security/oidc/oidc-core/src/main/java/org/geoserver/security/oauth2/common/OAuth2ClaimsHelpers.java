/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;

/**
 * Helpers for extracting claim values out of JWT/OIDC claim maps and normalizing them into a {@code List<String>}.
 *
 * <p>This is a self-contained replacement for the previously-borrowed helpers from the {@code jwt-headers} community
 * module. The OIDC plugin must not depend on a community module once it is promoted to extension status, so these two
 * static utilities (originally {@code JwtHeaderUserNameExtractor.getClaim} and
 * {@code JwtHeadersRolesExtractor.asStringList}) live here instead.
 *
 * <p>Both methods are deliberately conservative: they return {@code null} (or an empty list, for
 * {@link #asStringList(Object)} when the input is non-stringy) rather than throwing, so callers can keep using the
 * standard "no claim found ⇒ fall through" pattern.
 */
public final class OAuth2ClaimsHelpers {

    private OAuth2ClaimsHelpers() {
        // utility class
    }

    /**
     * Look up a claim by a dotted path, recursing into nested maps. Example: {@code getClaim(claims,
     * "address.country")} returns the {@code country} field of the {@code address} sub-claim.
     *
     * @param claims the JWT/OIDC claim map (or any {@code Map<String,Object>})
     * @param path dotted path, e.g. {@code "preferred_username"} or {@code "address.country"}
     * @return the value at that path, or {@code null} if any segment is missing
     */
    public static Object getClaim(Map<String, Object> claims, String path) {
        if (claims == null || path == null || path.isEmpty()) {
            return null;
        }
        return getClaim(claims, new ArrayList<>(Arrays.asList(path.split("\\."))));
    }

    @SuppressWarnings("unchecked")
    private static Object getClaim(Map<String, Object> claims, List<String> pathSegments) {
        if (claims == null || pathSegments.isEmpty()) {
            return null;
        }
        if (pathSegments.size() == 1) {
            return claims.get(pathSegments.get(0));
        }
        String head = pathSegments.get(0);
        pathSegments.remove(0);
        Object next = claims.get(head);
        if (next instanceof Map) {
            return getClaim((Map<String, Object>) next, pathSegments);
        }
        return null;
    }

    /**
     * Normalize a "list-of-strings"-ish claim value into a real {@code List<String>}. Accepts strings,
     * {@link JSONArray} (from json-path / json-smart), or arbitrary {@code List<?>} values; everything else returns an
     * empty list.
     *
     * @param raw raw claim value, may be {@code null}
     * @return an immutable-ish list; never {@code null}
     */
    public static List<String> asStringList(Object raw) {
        if (raw == null) {
            return Collections.emptyList();
        }
        if (raw instanceof String string) {
            return List.of(string);
        }
        if (raw instanceof JSONArray array) {
            return array.stream().map(Object::toString).collect(Collectors.toList());
        }
        if (raw instanceof List<?> list) {
            return list.stream().map(Object::toString).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
