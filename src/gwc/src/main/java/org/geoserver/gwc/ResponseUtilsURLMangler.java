/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import org.apache.commons.lang3.StringUtils;
import org.geoserver.gwc.dispatch.GwcServiceDispatcherCallback;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geowebcache.util.URLMangler;

public class ResponseUtilsURLMangler implements URLMangler {

    @Override
    public String buildURL(String baseURL, String contextPath, String path) {
        // In order to allow GWC to dispatch the requests the GwcServiceDispatcherCallback
        // puts the local workspace in the servlet context, however to build correct backlinks we
        // need to original base URL with the workspace stuck in the path so that the
        // proxy base URL rewrite won't eat it away
        final String originalBaseURL = GwcServiceDispatcherCallback.GWC_ORIGINAL_BASEURL.get();
        String base = originalBaseURL == null ? StringUtils.strip(baseURL, "/") : originalBaseURL;
        String cp = StringUtils.strip(contextPath, "/");
        String rest = cp + "/" + StringUtils.stripStart(path, "/");
        return ResponseUtils.buildURL(base, rest, null, URLType.SERVICE);
    }
}
