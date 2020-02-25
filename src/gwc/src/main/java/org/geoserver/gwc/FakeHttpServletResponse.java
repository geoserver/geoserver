/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.geotools.util.logging.Logging;

/** A HTTP response used when calling back into the GeoServer dispatcher */
@SuppressWarnings("deprecation")
public class FakeHttpServletResponse implements HttpServletResponse {

    private static Logger log = Logging.getLogger(HttpServletResponse.class.toString());

    private static class FakeServletOutputStream extends ServletOutputStream {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(20480);

        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        public byte[] getBytes() {
            return outputStream.toByteArray();
        }
    }

    private FakeServletOutputStream fos = new FakeServletOutputStream();

    private String contentType;

    private HashMap<String, String> headers = new HashMap<String, String>();

    private List<Cookie> cookies;

    private int responseCode = 200;

    public byte[] getBytes() {
        return fos.getBytes();
    }

    public Cookie[] getCachedCookies() {
        return cookies == null ? new Cookie[0] : cookies.toArray(new Cookie[cookies.size()]);
    }

    /** @see javax.servlet.http.HttpServletResponse#addCookie(javax.servlet.http.Cookie) */
    public void addCookie(Cookie cookie) {
        if (cookies == null) {
            cookies = new ArrayList<Cookie>(2);
        }
        cookies.add(cookie);
    }

    public void addDateHeader(String arg0, long arg1) {
        log.finer("Added date header: " + arg0 + " : " + arg1);
        headers.put(arg0, Long.toString(arg1));
    }

    public void addHeader(String arg0, String arg1) {
        log.finer("Added string header: " + arg0 + " : " + arg1);
        headers.put(arg0, arg1);
    }

    public void addIntHeader(String arg0, int arg1) {
        log.finer("Added integer header: " + arg0 + " : " + arg1);
        headers.put(arg0, Integer.toString(arg1));
    }

    public boolean containsHeader(String arg0) {
        return headers.containsKey(arg0);
    }

    public String encodeRedirectURL(String arg0) {
        throw new ServletDebugException();
    }

    public String encodeRedirectUrl(String arg0) {
        throw new ServletDebugException();
    }

    public String encodeURL(String arg0) {
        throw new ServletDebugException();
    }

    public String encodeUrl(String arg0) {
        throw new ServletDebugException();
    }

    public void sendError(int arg0) throws IOException {
        responseCode = arg0;
    }

    public void sendError(int arg0, String arg1) throws IOException {
        responseCode = arg0;
    }

    public void sendRedirect(String arg0) throws IOException {
        throw new ServletDebugException();
    }

    public void setDateHeader(String arg0, long arg1) {
        throw new ServletDebugException();
    }

    /** @see javax.servlet.http.HttpServletResponse#setHeader(java.lang.String, java.lang.String) */
    public void setHeader(String arg0, String arg1) {
        addHeader(arg0, arg1);
    }

    public void setIntHeader(String arg0, int arg1) {
        throw new ServletDebugException();
    }

    public void setStatus(int arg0) {
        throw new ServletDebugException();
    }

    public void setStatus(int arg0, String arg1) {
        throw new ServletDebugException();
    }

    @Override
    public int getStatus() {
        return responseCode;
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headers.containsKey(name)
                ? Arrays.asList(headers.get(name))
                : Collections.emptyList();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    public void flushBuffer() throws IOException {
        throw new ServletDebugException();
    }

    public int getBufferSize() {
        throw new ServletDebugException();
    }

    public String getCharacterEncoding() {
        throw new ServletDebugException();
    }

    public String getContentType() {
        return this.contentType;
    }

    public Locale getLocale() {
        throw new ServletDebugException();
    }

    public ServletOutputStream getOutputStream() throws IOException {
        log.finer("Returning output stream");
        return this.fos;
    }

    public PrintWriter getWriter() throws IOException {
        throw new ServletDebugException();
    }

    public boolean isCommitted() {
        throw new ServletDebugException();
    }

    public void reset() {
        throw new ServletDebugException();
    }

    public void resetBuffer() {
        throw new ServletDebugException();
    }

    public void setBufferSize(int arg0) {
        throw new ServletDebugException();
    }

    public void setCharacterEncoding(String arg0) {
        // throw new ServletDebugException();

    }

    public void setContentLength(int arg0) {
        throw new ServletDebugException();
    }

    public void setContentType(String arg0) {
        log.finer("Content type set to " + arg0);
        this.contentType = arg0;
    }

    public void setLocale(Locale arg0) {
        throw new ServletDebugException();
    }
}
