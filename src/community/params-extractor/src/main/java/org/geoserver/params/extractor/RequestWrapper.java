/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public final class RequestWrapper extends HttpServletRequestWrapper {

    private final UrlTransform urlTransform;
    private final Map originalParameters;

    private final String pathInfo;
    private final String servletPath;

    private final Map<String, String[]> parameters;

    public RequestWrapper(UrlTransform urlTransform, HttpServletRequest request) {
        super(request);
        this.urlTransform = urlTransform;
        originalParameters = request.getParameterMap();
        // extract URL portions for servletpath and pathinfo
        final Pattern pathsPattern =
                Pattern.compile(
                        "^"
                                + request.getContextPath()
                                + "(/[^?/#]+)(/[^?#]*[^/?#])?(?:/|/?[?#].*)?$");
        Matcher matcher = pathsPattern.matcher(urlTransform.getOriginalRequestUri());
        if (matcher.matches()) {
            servletPath = Optional.ofNullable(matcher.group(1)).orElse("");
            pathInfo = Optional.ofNullable(matcher.group(2)).orElse("");
        } else {
            pathInfo = "";
            servletPath = "";
        }
        parameters = new HashMap<>(super.getParameterMap());
        parameters.putAll(urlTransform.getParameters());
    }

    public Map getOriginalParameters() {
        return originalParameters;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    @Override
    public String getServletPath() {
        return servletPath;
    }

    public String getOriginalRequestURI() {
        return urlTransform.getOriginalRequestUri();
    }

    @Override
    public String getRequestURI() {
        return urlTransform.getRequestUri();
    }

    @Override
    public String getQueryString() {
        return urlTransform.getQueryString();
    }

    @Override
    public String getParameter(String name) {
        String[] value = parameters.get(name);
        if (value != null) {
            return value[0];
        }
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(final String name) {
        return getParameterMap().get(name);
    }
}
