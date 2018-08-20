/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import org.apache.commons.lang3.StringUtils;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.ows.util.ResponseUtils;
import org.geowebcache.util.URLMangler;

public class ResponseUtilsURLMangler implements URLMangler {

    @Override
    public String buildURL(String baseURL, String contextPath, String path) {
        String base = StringUtils.strip(baseURL, "/");
        String cp = "/" + StringUtils.strip(contextPath, "/");
        String rest = cp + "/" + StringUtils.stripStart(path, "/");
        return ResponseUtils.buildURL(base, rest, null, URLType.RESOURCE);
    }
}
