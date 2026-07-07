/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.gwc.dispatch.GwcServiceDispatcherCallback;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geowebcache.util.URLMangler;

public class ResponseUtilsURLMangler implements URLMangler {

    @Override
    public String buildURL(String baseURL, String contextPath, String path) {
        UrlParts urlParts = buildUrlParts(baseURL, contextPath, path);
        return ResponseUtils.buildURL(urlParts.baseUrl(), urlParts.restPath(), null, URLType.SERVICE);
    }

    /**
     * Resolves the GWC-specific base URL/path handling (see {@link #buildUrlParts}), then delegates to
     * {@link ResponseUtils#buildURL(String, String, Map, URLType)} for the actual mangler dispatch, kvp encoding and
     * query string composition, so that logic exists in a single authoritative place.
     *
     * <p>Callers of this method (GWC's WMTS capabilities/backlink generation) need the path and the query parameters
     * kept apart, since capability templates still contain unresolved placeholders such as {@code {TileRow}} at the
     * point the query string is appended. The full (already mangled and percent-encoded) URL produced by
     * {@code ResponseUtils.buildURL} is split back into its path and query components: the query component is decoded
     * into the returned {@link URLMangler.UrlAndParams#queryParameters()} and the path component becomes
     * {@link URLMangler.UrlAndParams#url()}. The {@code queryParameters} argument itself is never mutated.
     */
    @Override
    public UrlAndParams buildURL(String baseURL, String contextPath, String path, Map<String, String> queryParameters) {
        UrlParts urlParts = buildUrlParts(baseURL, contextPath, path);

        String fullUrl =
                ResponseUtils.buildURL(urlParts.baseUrl(), urlParts.restPath(), queryParameters, URLType.SERVICE);

        int queryIndex = fullUrl.indexOf('?');
        if (queryIndex == -1) {
            return new UrlAndParams(fullUrl, Collections.emptyMap());
        }

        Map<String, String> resultParameters = decodeQueryString(fullUrl.substring(queryIndex + 1));
        return new UrlAndParams(fullUrl.substring(0, queryIndex), resultParameters);
    }

    private static Map<String, String> decodeQueryString(String queryString) {
        Map<String, String> resultParameters = new LinkedHashMap<>();
        for (String pair : queryString.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq == -1 ? pair : pair.substring(0, eq);
            String value = eq == -1 ? "" : pair.substring(eq + 1);
            resultParameters.put(ResponseUtils.urlDecode(key), ResponseUtils.urlDecode(value));
        }
        return resultParameters;
    }

    private UrlParts buildUrlParts(String baseURL, String contextPath, String path) {
        // In order to allow GWC to dispatch the requests the GwcServiceDispatcherCallback
        // puts the local workspace in the servlet context, however to build correct backlinks we
        // need the original base URL with the workspace stuck in the path so that the
        // proxy base URL rewrite won't eat it away.
        final String originalBaseURL = GwcServiceDispatcherCallback.GWC_ORIGINAL_BASEURL.get();
        String base = originalBaseURL == null ? StringUtils.strip(baseURL, "/") : originalBaseURL;
        String cp = StringUtils.strip(contextPath, "/");
        String rest = cp + "/" + StringUtils.stripStart(path, "/");
        return new UrlParts(base, rest);
    }

    private record UrlParts(String baseUrl, String restPath) {}
}
