/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.geovolumes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.util.logging.Logging;

class LinkUpdater {

    static final Logger LOGGER = Logging.getLogger(LinkUpdater.class);

    public static String updateLink(String base, String path, String href) {
        if (!isRelative(href)) return href;
        if (href.contains(".."))
            throw new IllegalArgumentException(
                    "Illegal relative refernce from GeoVolume, should not contain '..': " + href);
        return ResponseUtils.buildURL(
                base, ResponseUtils.appendPath(path, href), Map.of(), URLMangler.URLType.SERVICE);
    }

    private static boolean isRelative(String href) {
        try {
            URI uri = new URI(href);
            if (uri.getScheme() == null) return true;
        } catch (URISyntaxException e) {
            LOGGER.log(Level.FINE, "Invalid URL found in GeoVolumes collections.json: " + href, e);
        }
        return false;
    }

    /**
     * Update any <code>href</code> property amongst the additional properties collected while
     * parsing the <code>collections.json</code> file.
     *
     * @param baseURL
     * @param basePath
     * @param additionalProperties
     */
    @SuppressWarnings("unchecked")
    public static void updateLinks(
            String baseURL, String basePath, Map<String, Object> additionalProperties) {
        // update href if any is found
        additionalProperties.computeIfPresent(
                "href", (k, v) -> updateLink(baseURL, basePath, (String) v));
        // find all properties whose value is a Map and recurse
        additionalProperties.values().stream()
                .filter(Map.class::isInstance)
                .forEach(m -> updateLinks(baseURL, basePath, (Map<String, Object>) m));
    }
}
