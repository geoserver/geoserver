/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

/**
 * Provides scope related utilities.
 *
 * @author awaterme
 */
public class ScopeUtils {

    /**
     * Turns a scope text, separated by comma or space into scopes
     *
     * @param pScopeList
     * @return An array, maybe empty
     */
    public static String[] valueOf(String pScopeList) {
        if (pScopeList == null || pScopeList.isBlank()) {
            return new String[] {};
        }
        if (pScopeList.contains(",")) {
            String[] lScopes = pScopeList.trim().split("\\s*,\\s*");
            return lScopes;
        }
        if (pScopeList.contains(" ")) {
            String[] lScopes = pScopeList.trim().split("\\s+");
            return lScopes;
        }
        return new String[] {pScopeList.trim()};
    }
}
