/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts external role names (from an identity provider) into internal GeoServer role names using a
 * GeoServer-configured mapping string.
 *
 * <p>This is a self-contained replacement for the previously-borrowed {@code RoleConverter} from the
 * {@code jwt-headers} community module. The OIDC plugin must not depend on a community module once it is promoted to
 * extension status.
 *
 * <p>The mapping is given as a single string of the form:
 *
 * <pre>
 *   "externalRoleName1=GeoServerRoleName1;externalRoleName2=GeoServerRoleName2"
 * </pre>
 *
 * <p>An external role may map to several internal roles by being listed multiple times. The
 * {@code onlyExternalListedRoles} flag controls what happens to external roles that are <em>not</em> in the map:
 *
 * <ul>
 *   <li>{@code true} → the external role is dropped entirely (strict allow-list mode);
 *   <li>{@code false} → the external role passes through unchanged (default).
 * </ul>
 *
 * <p>Role-name characters outside {@code [a-zA-Z0-9_.-]} are stripped to keep the mapping line robust against
 * accidental whitespace, quoting, or hostile input.
 */
public final class OAuth2RoleConverter {

    private static final String INVALID_NAME_CHARS = "[^a-zA-Z0-9_\\-\\.]";

    private final Map<String, List<String>> conversionMap;
    private final boolean externalNameMustBeListed;

    /**
     * @param roleConverterString mapping string in the form {@code "ext1=gs1;ext2=gs2"} — may be {@code null} or blank
     *     (no conversion happens, external roles pass through)
     * @param onlyExternalListedRoles if {@code true}, external roles not listed in the map are dropped
     */
    public OAuth2RoleConverter(String roleConverterString, boolean onlyExternalListedRoles) {
        this.conversionMap = parseRoleConverterString(roleConverterString);
        this.externalNameMustBeListed = onlyExternalListedRoles;
    }

    /**
     * Convert a list of external (IdP-issued) role names into internal GeoServer role names. Honors the
     * {@code onlyExternalListedRoles} flag.
     *
     * @param externalRoles roles from the IdP — may be {@code null}
     * @return converted roles; never {@code null}
     */
    public List<String> convert(List<String> externalRoles) {
        List<String> result = new ArrayList<>();
        if (externalRoles == null) {
            return result;
        }
        for (String externalRole : externalRoles) {
            List<String> mapped = conversionMap.get(externalRole);
            if (mapped == null) {
                if (!externalNameMustBeListed) {
                    result.add(externalRole);
                }
            } else {
                result.addAll(mapped);
            }
        }
        return result;
    }

    /** Parse the {@code "ext1=gs1;ext2=gs2"} mapping string into a map. Tolerant of blanks / malformed entries. */
    static Map<String, List<String>> parseRoleConverterString(String roleConverterString) {
        if (roleConverterString == null || roleConverterString.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> result = new HashMap<>();
        for (String part : roleConverterString.split(";")) {
            String[] kv = part.split("=");
            if (kv.length != 2) {
                continue;
            }
            String key = sanitize(kv[0]);
            String val = sanitize(kv[1]);
            if (key.isBlank() || val.isBlank()) {
                continue;
            }
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
        }
        return result;
    }

    private static String sanitize(String s) {
        return s.replaceAll(INVALID_NAME_CHARS, "");
    }
}
