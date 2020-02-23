/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.ows.URLMangler;
import org.geoserver.ows.util.ResponseUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * An object which contains information about the "page" or "resource" being accessed in a rest
 * request.
 *
 * <p>Equivalent of PageInfo used by the old rest module.
 *
 * <p>An instance of this class can be referenced by any restlet via:
 *
 * <pre>
 * RequestContextHolder.getRequestAttributes().getAttribute( RequestInfo.KEY, RequestAttributes.SCOPE_REQUEST );
 * </pre>
 */
public class RequestInfo {

    /** key to reference this object by */
    public static final String KEY = "RequestInfo";

    String baseURL;
    String servletPath;
    String pagePath;
    String extension;

    private Map<String, String[]> queryMap;

    /** Constructs an empty {@link RequestInfo} object */
    public RequestInfo() {}

    /** Constructs a {@link RequestInfo} object, generating content based on the passed request. */
    public RequestInfo(HttpServletRequest request) {
        // http://host:port/appName
        baseURL =
                request.getRequestURL()
                        .toString()
                        .replace(request.getRequestURI(), request.getContextPath());

        servletPath = request.getServletPath();
        pagePath = request.getServletPath() + request.getPathInfo();
        setQueryMap(request.getParameterMap());
        // strip off the extension
        extension = ResponseUtils.getExtension(pagePath);
        if (extension != null) {
            pagePath = pagePath.substring(0, pagePath.length() - extension.length() - 1);
        }

        // trim leading slash
        if (pagePath.endsWith("/")) {
            pagePath = pagePath.substring(0, pagePath.length() - 1);
        }
    }

    private void setQueryMap(Map<String, String[]> parameterMap) {
        queryMap = parameterMap;
    }

    /** Gets the base URL of the server, e.g. "http://localhost:8080/geoserver" */
    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /** Gets the relative path to the servlet, e.g. "/rest" */
    public String getServletPath() {
        return servletPath;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    /** Gets the relative path to the current page, e.g. "rest/layers" */
    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

    /** Gets the extension for the currnet page, e.g. "xml" */
    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String pageURI(String path) {
        return buildURI(pagePath, path);
    }

    public String servletURI(String path) {
        return buildURI(servletPath, path);
    }

    String buildURI(String base, String path) {
        if (path != null) {
            if (path.startsWith(".")) {
                if (base.endsWith("/")) base = base.substring(1);
                path = base + path;
            } else {
                path = ResponseUtils.appendPath(base, path);
            }
        }

        return ResponseUtils.buildURL(baseURL, path, null, URLMangler.URLType.SERVICE);
    }

    /** Returns the RequestInfo from the current {@link RequestContextHolder} */
    public static RequestInfo get() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) return null;
        return (RequestInfo)
                requestAttributes.getAttribute(RequestInfo.KEY, RequestAttributes.SCOPE_REQUEST);
    }

    public Map<String, String[]> getQueryMap() {
        return queryMap;
    }

    /** Sets the provided RequestInfo into the {@link RequestContextHolder} */
    public static void set(RequestInfo requestInfo) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            throw new IllegalStateException("Request attributes are not set");
        }
        requestAttributes.setAttribute(
                RequestInfo.KEY, requestInfo, RequestAttributes.SCOPE_REQUEST);
    }
}
