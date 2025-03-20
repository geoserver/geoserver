/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.io.BufferedReader;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class FakeHttpServletRequest implements HttpServletRequest {

    private final String workspace;

    private final Map<String, String[]> parameterMap;

    private final Cookie[] cookies;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<HttpServletRequest> original;

    public FakeHttpServletRequest(Map<String, String> parameterMap, Cookie[] cookies) {
        this(parameterMap, cookies, null);
    }

    public FakeHttpServletRequest(Map<String, String> parameterMap, Cookie[] cookies, String workspace) {
        this.parameterMap = parameterMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new String[] {e.getValue()}));
        this.cookies = cookies;
        this.workspace = workspace;
        // grab the original request from Spring to forward security related attributes
        // such as requests host, ports and headers
        this.original = Optional.ofNullable((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                .map(ServletRequestAttributes::getRequest);
    }

    /** Standard interface */
    @Override
    public String getAuthType() {
        throw new ServletDebugException();
    }

    @Override
    public String getContextPath() {
        return "/geoserver";
    }

    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    @Override
    public long getDateHeader(String name) {
        return original.map(r -> r.getDateHeader(name)).orElseThrow(ServletDebugException::new);
    }

    @Override
    public String getHeader(String name) {
        return original.map(r -> r.getHeader(name)).orElse(null);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return original.map(HttpServletRequest::getHeaderNames).orElse(Collections.emptyEnumeration());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return original.map(r -> r.getHeaders(name)).orElseThrow(ServletDebugException::new);
    }

    @Override
    public int getIntHeader(String name) {
        return original.map(r -> r.getIntHeader(name)).orElseThrow(ServletDebugException::new);
    }

    @Override
    public String getMethod() {
        return "GET";
    }

    @Override
    public String getPathInfo() {
        throw new ServletDebugException();
    }

    @Override
    public String getPathTranslated() {
        throw new ServletDebugException();
    }

    @Override
    public String getQueryString() {
        throw new ServletDebugException();
    }

    @Override
    public String getRemoteUser() {
        throw new ServletDebugException();
    }

    @Override
    public String getRequestURI() {
        if (workspace != null && !workspace.isEmpty()) {
            return "/geoserver/" + workspace + "/wms";
        } else {
            return "/geoserver/wms";
        }
    }

    @Override
    public StringBuffer getRequestURL() {
        throw new ServletDebugException();
    }

    @Override
    public String getRequestedSessionId() {
        throw new ServletDebugException();
    }

    @Override
    public String getServletPath() {
        return "/wms";
    }

    @Override
    public HttpSession getSession() {
        throw new ServletDebugException();
    }

    @Override
    public String changeSessionId() {
        throw new ServletDebugException();
    }

    @Override
    public HttpSession getSession(boolean arg0) {
        throw new ServletDebugException();
    }

    @Override
    public Principal getUserPrincipal() {
        throw new ServletDebugException();
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        throw new ServletDebugException();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        throw new ServletDebugException();
    }

    /** @deprecated use #isRequestedSessionIdFromURL() */
    @Deprecated
    @SuppressWarnings({"deprecation"})
    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {}

    @Override
    public void logout() {}

    @Override
    public Collection<Part> getParts() {
        return null;
    }

    @Override
    public Part getPart(String name) {
        return null;
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) {
        return null;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        throw new ServletDebugException();
    }

    @Override
    public boolean isUserInRole(String arg0) {
        throw new ServletDebugException();
    }

    @Override
    public Object getAttribute(String arg0) {
        throw new ServletDebugException();
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        throw new ServletDebugException();
    }

    @Override
    public String getCharacterEncoding() {
        return "UTF-8";
    }

    @Override
    public int getContentLength() {
        throw new ServletDebugException();
    }

    @Override
    public long getContentLengthLong() {
        return 0;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public ServletInputStream getInputStream() {
        throw new ServletDebugException();
    }

    @Override
    public String getLocalAddr() {
        return original.map(ServletRequest::getLocalAddr).orElseThrow(ServletDebugException::new);
    }

    @Override
    public String getLocalName() {
        return original.map(ServletRequest::getLocalName).orElseThrow(ServletDebugException::new);
    }

    @Override
    public int getLocalPort() {
        return original.map(ServletRequest::getLocalPort).orElse(0);
    }

    @Override
    public ServletContext getServletContext() {
        return original.map(ServletRequest::getServletContext).orElse(null);
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        throw new ServletDebugException();
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        throw new ServletDebugException();
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    @Override
    public Locale getLocale() {
        return original.map(ServletRequest::getLocale).orElseThrow(ServletDebugException::new);
    }

    @Override
    public Enumeration<Locale> getLocales() {
        throw new ServletDebugException();
    }

    @Override
    public String getParameter(String name) {
        String[] value = parameterMap.get(name);
        if (value == null || value.length == 0) return null;
        return value[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    @Override
    public String getProtocol() {
        return original.map(ServletRequest::getProtocol).orElseThrow(ServletDebugException::new);
    }

    @Override
    public BufferedReader getReader() {
        throw new ServletDebugException();
    }

    /** @deprecated */
    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
    public String getRealPath(String arg0) {
        throw new ServletDebugException();
    }

    @Override
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public String getRemoteAddr() {
        return original.map(ServletRequest::getRemoteAddr).orElse("127.0.0.1");
    }

    @Override
    public String getRemoteHost() {
        return original.map(ServletRequest::getRemoteHost).orElse("localhost");
    }

    @Override
    public int getRemotePort() {
        return original.map(ServletRequest::getRemotePort).orElseThrow(ServletDebugException::new);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new ServletDebugException();
    }

    @Override
    public String getScheme() {
        return original.map(ServletRequest::getScheme).orElse("http");
    }

    @Override
    public String getServerName() {
        return original.map(ServletRequest::getServerName).orElse("localhost");
    }

    @Override
    public int getServerPort() {
        return original.map(ServletRequest::getServerPort).orElse(8080);
    }

    @Override
    public boolean isSecure() {
        return original.map(ServletRequest::isSecure).orElseThrow(ServletDebugException::new);
    }

    @Override
    public void removeAttribute(String arg0) {
        throw new ServletDebugException();
    }

    @Override
    public void setAttribute(String arg0, Object arg1) {
        throw new ServletDebugException();
    }

    @Override
    public void setCharacterEncoding(String arg0) {
        if (!arg0.equals("UTF-8")) {
            throw new ServletDebugException();
        }
    }
}
