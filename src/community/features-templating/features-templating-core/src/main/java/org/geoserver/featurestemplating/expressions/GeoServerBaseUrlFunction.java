/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.filter.capability.FunctionNameImpl;

/**
 * Builds a URL based on the base URL of the GeoServer instance. This is useful for generating links to resources in the
 * GeoServer instance.
 *
 * <p>Example usage:
 *
 * <pre>
 * ${geoServerBaseUrl()}
 * </pre>
 */
public class GeoServerBaseUrlFunction extends RequestFunction {

    public static FunctionName NAME = new FunctionNameImpl("geoServerBaseUrl", parameter("result", String.class));

    public GeoServerBaseUrlFunction() {
        super(NAME);
    }

    @Override
    protected Object evaluateInternal(Request request, Object object) {
        String baseURL = ResponseUtils.baseURL(request.getHttpRequest());
        String url = ResponseUtils.buildURL(baseURL, "", null, URLMangler.URLType.RESOURCE);
        // remote trailing slash character if exists
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
