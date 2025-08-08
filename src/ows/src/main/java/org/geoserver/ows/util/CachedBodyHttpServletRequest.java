/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * {@code HttpServletRequest} wrapper that caches the request body in memory so it can be re-read multiple times (e.g.,
 * once for lightweight XML sniffing and again by the actual request parser).
 *
 * <p>This wrapper:
 *
 * <ul>
 *   <li>Buffers the entire request body into a byte array (up to a configurable maximum),
 *   <li>Returns fresh {@link ServletInputStream} instances backed by the cached bytes,
 *   <li>Honors the original request's declared character encoding for {@link #getReader()},
 *   <li>Overrides {@link #getContentLength()} and {@link #getContentLengthLong()} to report the cached body size.
 * </ul>
 *
 * <p><strong>Memory note:</strong> The whole request body is stored in memory. Use {@link #wrap(HttpServletRequest,
 * long)} with a sensible {@code maxBytes} limit to avoid large allocations. If the request exceeds the limit, the
 * original request is returned unchanged.
 *
 * <p>Typical usage:
 *
 * <pre>{@code
 * HttpServletRequest effective =
 *     CachedBodyHttpServletRequest.wrap(originalRequest, 2_000_000L); // 2 MB cap
 * }</pre>
 */
public final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final byte[] body;
    private BufferedReader cachedReader;

    /**
     * Wraps the provided request in a {@code CachedBodyHttpServletRequest} if the body size is at or below
     * {@code maxBytes}. If the size is unknown or larger than {@code maxBytes}, the original request is returned
     * unmodified.
     *
     * <p>This method reads and buffers the entire request input stream (closing it in the process). The returned
     * wrapper supplies repeatable input streams/readers over the cached bytes.
     *
     * @param req the incoming {@link HttpServletRequest}
     * @param maxBytes maximum number of bytes to buffer in memory; if exceeded, no wrapping occurs
     * @return a caching wrapper, or {@code req} if the limit would be exceeded
     * @throws IOException if the request body cannot be read
     */
    public static HttpServletRequest wrap(HttpServletRequest req, long maxBytes) throws IOException {
        long cl = req.getContentLengthLong();
        if (cl > 0 && cl > maxBytes) return req;
        try (InputStream in = req.getInputStream();
                ByteArrayOutputStream baos =
                        new ByteArrayOutputStream(cl > 0 && cl < Integer.MAX_VALUE ? (int) cl : 8192)) {
            byte[] buf = new byte[8192];
            long total = 0;
            for (int r; (r = in.read(buf)) != -1; ) {
                total += r;
                if (total > maxBytes) return req; // safeguard
                baos.write(buf, 0, r);
            }
            return new CachedBodyHttpServletRequest(req, baos.toByteArray());
        }
    }

    private CachedBodyHttpServletRequest(HttpServletRequest req, byte[] body) {
        super(req);
        this.body = body;
    }

    /**
     * Returns a fresh {@link ServletInputStream} over the cached bytes. Each call creates an independent stream
     * positioned at the beginning of the cached body.
     */
    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return bais.read();
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return bais.read(b, off, len);
            }

            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener rl) {}
        };
    }

    /**
     * Returns a {@link BufferedReader} over the cached body, using the request's declared character encoding if
     * present, otherwise UTF-8. The same reader instance is cached and returned on subsequent calls.
     *
     * @return a {@code BufferedReader} for the cached body
     * @throws IOException if the reader cannot be created
     */
    @Override
    public BufferedReader getReader() throws IOException {
        if (cachedReader == null) {
            String enc = getCharacterEncoding();
            Charset cs = enc != null ? Charset.forName(enc) : StandardCharsets.UTF_8;
            cachedReader = new BufferedReader(new InputStreamReader(getInputStream(), cs));
        }
        return cachedReader;
    }

    /** Returns the cached body length, in bytes. */
    @Override
    public int getContentLength() {
        return body.length;
    }

    /** Returns the cached body length, in bytes. */
    @Override
    public long getContentLengthLong() {
        return body.length;
    }

    /**
     * Returns the raw cached body bytes. Callers must not modify the returned array.
     *
     * @return the cached request body
     */
    public byte[] getBody() {
        return body;
    }
}
