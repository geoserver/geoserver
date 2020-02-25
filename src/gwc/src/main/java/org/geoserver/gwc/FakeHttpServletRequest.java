/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
import javax.servlet.http.Part;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SuppressWarnings({"rawtypes", "deprecation"})
class FakeHttpServletRequest implements HttpServletRequest {

    private static final Enumeration EMPTY_ENUMERATION =
            new Enumeration() {
                @Override
                public boolean hasMoreElements() {
                    // TODO Auto-generated method stub
                    return false;
                }

                @Override
                public Object nextElement() {
                    // TODO Auto-generated method stub
                    return null;
                }
            };

    private String workspace;

    private Map<String, String> parameterMap;

    private Cookie[] cookies;

    private Optional<HttpServletRequest> original;

    public FakeHttpServletRequest(Map<String, String> parameterMap, Cookie[] cookies) {
        this(parameterMap, cookies, null);
    }

    public FakeHttpServletRequest(
            Map<String, String> parameterMap, Cookie[] cookies, String workspace) {
        this.parameterMap = parameterMap;
        this.cookies = cookies;
        this.workspace = workspace;
        // grab the original request from Spring to forward security related attributes
        // such as requests host, ports and headers
        this.original =
                Optional.ofNullable(
                                (ServletRequestAttributes)
                                        RequestContextHolder.getRequestAttributes())
                        .map(ServletRequestAttributes::getRequest);
    }

    /** Standard interface */
    public String getAuthType() {
        throw new ServletDebugException();
    }

    public String getContextPath() {
        return "/geoserver";
    }

    public Cookie[] getCookies() {
        return cookies;
    }

    public long getDateHeader(String name) {
        return original.map(r -> r.getDateHeader(name))
                .orElseThrow(() -> new ServletDebugException());
    }

    public String getHeader(String name) {
        return original.map(r -> r.getHeader(name)).orElse(null);
    }

    public Enumeration getHeaderNames() {
        return original.map(r -> r.getHeaderNames()).orElse(EMPTY_ENUMERATION);
    }

    public Enumeration getHeaders(String name) {
        return original.map(r -> r.getHeaders(name)).orElseThrow(() -> new ServletDebugException());
    }

    public int getIntHeader(String name) {
        return original.map(r -> r.getIntHeader(name))
                .orElseThrow(() -> new ServletDebugException());
    }

    public String getMethod() {
        return "GET";
    }

    public String getPathInfo() {
        throw new ServletDebugException();
    }

    public String getPathTranslated() {
        throw new ServletDebugException();
    }

    public String getQueryString() {
        throw new ServletDebugException();
    }

    public String getRemoteUser() {
        throw new ServletDebugException();
    }

    public String getRequestURI() {
        if (workspace != null && !workspace.isEmpty()) {
            return "/geoserver/" + workspace + "/wms";
        } else {
            return "/geoserver/wms";
        }
    }

    public StringBuffer getRequestURL() {
        throw new ServletDebugException();
    }

    public String getRequestedSessionId() {
        throw new ServletDebugException();
    }

    public String getServletPath() {
        throw new ServletDebugException();
    }

    public HttpSession getSession() {
        throw new ServletDebugException();
    }

    public HttpSession getSession(boolean arg0) {
        throw new ServletDebugException();
    }

    public Principal getUserPrincipal() {
        throw new ServletDebugException();
    }

    public boolean isRequestedSessionIdFromCookie() {
        throw new ServletDebugException();
    }

    public boolean isRequestedSessionIdFromURL() {
        throw new ServletDebugException();
    }

    public boolean isRequestedSessionIdFromUrl() {
        throw new ServletDebugException();
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {}

    @Override
    public void logout() throws ServletException {}

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return null;
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        return null;
    }

    public boolean isRequestedSessionIdValid() {
        throw new ServletDebugException();
    }

    public boolean isUserInRole(String arg0) {
        throw new ServletDebugException();
    }

    public Object getAttribute(String arg0) {
        throw new ServletDebugException();
    }

    public Enumeration getAttributeNames() {
        throw new ServletDebugException();
    }

    public String getCharacterEncoding() {
        return "UTF-8";
    }

    public int getContentLength() {
        throw new ServletDebugException();
    }

    public String getContentType() {
        return null;
    }

    public ServletInputStream getInputStream() throws IOException {
        throw new ServletDebugException();
    }

    public String getLocalAddr() {
        return original.map(r -> r.getLocalAddr()).orElseThrow(() -> new ServletDebugException());
    }

    public String getLocalName() {
        return original.map(r -> r.getLocalName()).orElseThrow(() -> new ServletDebugException());
    }

    public int getLocalPort() {
        return original.map(r -> r.getLocalPort()).orElse(0);
    }

    @Override
    public ServletContext getServletContext() {
        return original.map(r -> r.getServletContext()).orElse(null);
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

    public Locale getLocale() {
        return original.map(r -> r.getLocale()).orElseThrow(() -> new ServletDebugException());
    }

    public Enumeration getLocales() {
        throw new ServletDebugException();
    }

    public String getParameter(String name) {
        return parameterMap.get(name);
    }

    public Map getParameterMap() {
        return parameterMap;
    }

    public Enumeration getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    public String[] getParameterValues(String name) {
        return new String[] {parameterMap.get(name)};
    }

    public String getProtocol() {
        return original.map(r -> r.getProtocol()).orElseThrow(() -> new ServletDebugException());
    }

    public BufferedReader getReader() throws IOException {
        throw new ServletDebugException();
    }

    public String getRealPath(String arg0) {
        throw new ServletDebugException();
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public String getRemoteAddr() {
        return original.map(r -> r.getRemoteAddr()).orElse("127.0.0.1");
    }

    public String getRemoteHost() {
        return original.map(r -> r.getRemoteHost()).orElse("localhost");
    }

    public int getRemotePort() {
        return original.map(r -> r.getRemotePort()).orElseThrow(() -> new ServletDebugException());
    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new ServletDebugException();
    }

    public String getScheme() {
        return original.map(r -> r.getScheme()).orElse("http");
    }

    public String getServerName() {
        return original.map(r -> r.getServerName()).orElse("localhost");
    }

    public int getServerPort() {
        return original.map(r -> r.getServerPort()).orElse(8080);
    }

    public boolean isSecure() {
        return original.map(r -> r.isSecure()).orElseThrow(() -> new ServletDebugException());
    }

    public void removeAttribute(String arg0) {
        throw new ServletDebugException();
    }

    public void setAttribute(String arg0, Object arg1) {
        throw new ServletDebugException();
    }

    public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {
        if (!arg0.equals("UTF-8")) {
            throw new ServletDebugException();
        }
    }
}
