/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.ows.Dispatcher;
import org.geotools.util.logging.Logging;

/**
 * A minimal HttpServletResponse implementation used to capture output when {@link GWC} dispatches internal requests to
 * the GeoServer OWS {@link Dispatcher}.
 *
 * <p>This class serves as a bridge/adapter that allows GWC to programmatically invoke GeoServer's dispatcher and
 * capture the response in memory without sending it over an actual HTTP connection. The {@link #getBytes() response
 * bytes} can then be cached, returned to the client, or further processed.
 *
 * <h3>Why This Class Exists</h3>
 *
 * When GWC generates cached tiles by invoking GeoServer's rendering pipeline internally, it needs a way to capture the
 * rendered image output. This class collects the response bytes into a {@link ByteArrayOutputStream} that can be
 * retrieved later via {@link #getBytes()}.
 *
 * <h3>How It's Used</h3>
 *
 * Primary usage occurs in:
 *
 * <ul>
 *   <li>{@link GWC#dispatchOwsRequest(Map, Cookie[])} - Captures dispatcher output for internal requests
 *   <li>The response bytes are wrapped in a {@link org.geoserver.platform.resource.Resource} for further processing or
 *       caching
 * </ul>
 *
 * <h3>Key Characteristics</h3>
 *
 * <ul>
 *   <li>Captures response output via an internal {@link ByteArrayOutputStream}
 *   <li>Stores headers, cookies, content type, and response codes
 *   <li>Most unimplemented methods throw {@link ServletDebugException} to catch misuse
 *   <li>Response bytes are retrievable via {@link #getBytes()} after dispatcher completes
 *   <li>Initial buffer size is 20KB (optimized for typical WMS tile responses)
 * </ul>
 *
 * @see InternalDispatchServletRequest
 * @see GWC#dispatchOwsRequest(Map, Cookie[])
 */
@SuppressWarnings("deprecation")
public class InternalDispatchServletResponse implements HttpServletResponse {

    private static Logger log = Logging.getLogger(HttpServletResponse.class.toString());

    private static class InternalDispatchServletOutputStream extends ServletOutputStream {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(20480);

        @Override
        public void write(int b) throws IOException {
            outputStream.write(b);
        }

        public byte[] getBytes() {
            return outputStream.toByteArray();
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {}
    }

    private InternalDispatchServletOutputStream fos = new InternalDispatchServletOutputStream();

    private String contentType;

    private HashMap<String, String> headers = new HashMap<>();

    private List<Cookie> cookies;

    private int responseCode = 200;

    public byte[] getBytes() {
        return fos.getBytes();
    }

    public Cookie[] getCachedCookies() {
        return cookies == null ? new Cookie[0] : cookies.toArray(new Cookie[cookies.size()]);
    }

    /** @see jakarta.servlet.http.HttpServletResponse#addCookie(jakarta.servlet.http.Cookie) */
    @Override
    public void addCookie(Cookie cookie) {
        if (cookies == null) {
            cookies = new ArrayList<>(2);
        }
        cookies.add(cookie);
    }

    @Override
    public void addDateHeader(String arg0, long arg1) {
        log.finer("Added date header: " + arg0 + " : " + arg1);
        headers.put(arg0, Long.toString(arg1));
    }

    @Override
    public void addHeader(String arg0, String arg1) {
        log.finer("Added string header: " + arg0 + " : " + arg1);
        headers.put(arg0, arg1);
    }

    @Override
    public void addIntHeader(String arg0, int arg1) {
        log.finer("Added integer header: " + arg0 + " : " + arg1);
        headers.put(arg0, Integer.toString(arg1));
    }

    @Override
    public boolean containsHeader(String arg0) {
        return headers.containsKey(arg0);
    }

    @Override
    public String encodeRedirectURL(String arg0) {
        throw new ServletDebugException();
    }

    @Override
    public String encodeURL(String arg0) {
        throw new ServletDebugException();
    }

    @Override
    public void sendError(int arg0) throws IOException {
        responseCode = arg0;
    }

    @Override
    public void sendError(int arg0, String arg1) throws IOException {
        responseCode = arg0;
    }

    @Override
    public void sendRedirect(String arg0) throws IOException {
        throw new ServletDebugException();
    }

    @Override
    public void sendRedirect(String s, int i, boolean b) throws IOException {
        throw new ServletDebugException();
    }

    @Override
    public void setDateHeader(String arg0, long arg1) {
        throw new ServletDebugException();
    }

    /** @see jakarta.servlet.http.HttpServletResponse#setHeader(java.lang.String, java.lang.String) */
    @Override
    public void setHeader(String arg0, String arg1) {
        addHeader(arg0, arg1);
    }

    @Override
    public void setIntHeader(String arg0, int arg1) {
        throw new ServletDebugException();
    }

    @Override
    public void setStatus(int arg0) {
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
        return headers.containsKey(name) ? Arrays.asList(headers.get(name)) : Collections.emptyList();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public void flushBuffer() throws IOException {
        throw new ServletDebugException();
    }

    @Override
    public int getBufferSize() {
        throw new ServletDebugException();
    }

    @Override
    public String getCharacterEncoding() {
        throw new ServletDebugException();
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public Locale getLocale() {
        throw new ServletDebugException();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        log.finer("Returning output stream");
        return this.fos;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        throw new ServletDebugException();
    }

    @Override
    public boolean isCommitted() {
        throw new ServletDebugException();
    }

    @Override
    public void reset() {
        throw new ServletDebugException();
    }

    @Override
    public void resetBuffer() {
        throw new ServletDebugException();
    }

    @Override
    public void setBufferSize(int arg0) {
        throw new ServletDebugException();
    }

    @Override
    public void setCharacterEncoding(String arg0) {
        // throw new ServletDebugException();

    }

    @Override
    public void setContentLength(int arg0) {
        throw new ServletDebugException();
    }

    @Override
    public void setContentLengthLong(long l) {}

    @Override
    public void setContentType(String arg0) {
        log.finer("Content type set to " + arg0);
        this.contentType = arg0;
    }

    @Override
    public void setLocale(Locale arg0) {
        throw new ServletDebugException();
    }
}
