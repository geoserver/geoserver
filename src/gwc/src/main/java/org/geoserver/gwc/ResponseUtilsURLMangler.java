/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.gwc.dispatch.GwcServiceDispatcherCallback;
import org.geoserver.ows.util.ResponseUtils;
import org.geowebcache.util.URLMangler;

/** Adapts GeoWebCache URL callbacks to GeoServer's URL mangling implementation. */
public class ResponseUtilsURLMangler implements URLMangler {

    @Override
    public void mangleURL(StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLMangler.URLType type) {
        String originalBaseURL = GwcServiceDispatcherCallback.GWC_ORIGINAL_BASEURL.get();
        String base = originalBaseURL == null ? StringUtils.strip(baseURL.toString(), "/") : originalBaseURL;
        org.geoserver.ows.URLMangler.URLType geoServerType = org.geoserver.ows.URLMangler.URLType.valueOf(type.name());
        String result = ResponseUtils.buildURL(base, path.toString(), kvp, geoServerType);
        baseURL.setLength(0);
        baseURL.append(result);
        path.setLength(0);
        kvp.clear();
    }
}
