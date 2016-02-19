/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParamsExtractorUrlMangler implements URLMangler {

    private static final Pattern URI_PATTERN = Pattern.compile("^((?:/)[^/]+/)(.*)$");

    @Override
    public void mangleURL(StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        Request request = Dispatcher.REQUEST.get();
        if (request == null || !"GetCapabilities".equalsIgnoreCase(request.getRequest())) {
            return;
        }
        String requestUri = request.getHttpRequest().getRequestURI();
        if (request.getHttpRequest() instanceof RequestWrapper) {
            requestUri = ((RequestWrapper) request.getHttpRequest()).getOriginalRequestURI();
        }
        Matcher matcher = URI_PATTERN.matcher(requestUri);
        if (!matcher.matches()) {
            return;
        }
        path.delete(0, path.length());
        path.append(matcher.group(2));
    }
}