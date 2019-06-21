/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.resource.Resource;

public class UrlMangler implements URLMangler {

    private static final Pattern URI_PATTERN = Pattern.compile("^((?:/)[^/]+/)(.*)$");

    private List<EchoParameter> echoParameters;

    public UrlMangler(GeoServerDataDirectory dataDirectory) {
        Resource resource = dataDirectory.get(EchoParametersDao.getEchoParametersPath());
        echoParameters = EchoParametersDao.getEchoParameters(resource.in());
        resource.addListener(
                notify -> echoParameters = EchoParametersDao.getEchoParameters(resource.in()));
    }

    private HttpServletRequest getHttpRequest(Request request) {
        HttpServletRequest httpRequest = request.getHttpRequest();
        while (httpRequest instanceof HttpServletRequestWrapper
                && !(httpRequest instanceof RequestWrapper)) {
            ServletRequest servlet = ((HttpServletRequestWrapper) httpRequest).getRequest();
            if (servlet instanceof HttpServletRequest) {
                httpRequest = (HttpServletRequest) servlet;
            } else {
                throw new RuntimeException("Only HttpRequest is supported");
            }
        }
        return httpRequest;
    }

    @Override
    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        Request request = Dispatcher.REQUEST.get();
        if (request == null || !"GetCapabilities".equalsIgnoreCase(request.getRequest())) {
            return;
        }
        forwardOriginalUri(request, path);
        Map requestRawKvp = request.getRawKvp();
        HttpServletRequest httpRequest = getHttpRequest(request);
        if (httpRequest instanceof RequestWrapper) {
            RequestWrapper requestWrapper = (RequestWrapper) httpRequest;
            Map parameters = requestWrapper.getOriginalParameters();
            requestRawKvp = new KvpMap(KvpUtils.normalize(parameters));
        }
        forwardParameters(requestRawKvp, kvp);
    }

    private void forwardOriginalUri(Request request, StringBuilder path) {
        HttpServletRequest httpRequest = getHttpRequest(request);
        String requestUri = httpRequest.getRequestURI();
        if (httpRequest instanceof RequestWrapper) {
            requestUri = ((RequestWrapper) httpRequest).getOriginalRequestURI();
        }
        Matcher matcher = URI_PATTERN.matcher(requestUri);
        if (!matcher.matches()) {
            return;
        }
        path.delete(0, path.length());
        path.append(matcher.group(2));
    }

    private void forwardParameters(Map requestRawKvp, Map<String, String> kvp) {
        for (EchoParameter echoParameter : echoParameters) {
            if (!echoParameter.getActivated()) {
                continue;
            }
            Map.Entry rawParameter =
                    Utils.caseInsensitiveSearch(echoParameter.getParameter(), requestRawKvp);
            if (rawParameter != null
                    && Utils.caseInsensitiveSearch(echoParameter.getParameter(), kvp) == null) {
                if (rawParameter.getValue() instanceof String) {
                    kvp.put((String) rawParameter.getKey(), (String) rawParameter.getValue());
                }
            }
        }
    }
}
