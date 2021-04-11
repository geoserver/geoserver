/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
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
import org.geotools.util.logging.Logging;

public class UrlMangler implements URLMangler {

    private static final Logger LOGGER = Logging.getLogger(UrlMangler.class);

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
            Utils.debug(
                    LOGGER,
                    "Not a capabilities request, ignored by the parameters extractor URL mangler.");
            return;
        }
        forwardOriginalUri(request, path);
        Map<String, Object> requestRawKvp = request.getRawKvp();
        HttpServletRequest httpRequest = getHttpRequest(request);
        if (httpRequest instanceof RequestWrapper) {
            RequestWrapper requestWrapper = (RequestWrapper) httpRequest;
            Map<String, String[]> parameters = requestWrapper.getOriginalParameters();
            requestRawKvp = new KvpMap<>(KvpUtils.normalize(parameters));
        }
        forwardParameters(requestRawKvp, kvp);
        Utils.debug(LOGGER, "Parameters extractor URL mangler applied.");
    }

    private void forwardOriginalUri(Request request, StringBuilder path) {
        HttpServletRequest httpRequest = getHttpRequest(request);
        String requestUri = httpRequest.getRequestURI();
        if (httpRequest instanceof RequestWrapper) {
            requestUri = ((RequestWrapper) httpRequest).getOriginalRequestURI();
        }
        int i = httpRequest.getContextPath().length() + 1;
        String pathInfo = requestUri.substring(i);
        path.delete(0, path.length());
        path.append(pathInfo);
    }

    private void forwardParameters(Map<String, Object> requestRawKvp, Map<String, String> kvp) {
        for (EchoParameter echoParameter : echoParameters) {
            if (!echoParameter.getActivated()) {
                continue;
            }
            Map.Entry<String, Object> rawParameter =
                    Utils.caseInsensitiveSearch(echoParameter.getParameter(), requestRawKvp);
            if (rawParameter != null
                    && Utils.caseInsensitiveSearch(echoParameter.getParameter(), kvp) == null) {
                if (rawParameter.getValue() instanceof String) {
                    kvp.put(rawParameter.getKey(), (String) rawParameter.getValue());
                }
            }
        }
    }
}
