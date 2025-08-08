/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Servlet filter that eagerly caches request bodies for non-idempotent HTTP methods (everything except {@code GET} and
 * {@code HEAD}) so downstream code can safely call {@link javax.servlet.ServletRequest#getInputStream()} or
 * {@link javax.servlet.ServletRequest#getReader()} multiple times.
 *
 * <p>The filter wraps the incoming {@link HttpServletRequest} with {@link CachedBodyHttpServletRequest} at the very
 * start of the chain. This mitigates issues with proxies/load balancers that deliver request bodies in small or delayed
 * chunks, and it prevents premature consumption of the input stream by earlier components (e.g., loggers, sniffers).
 *
 * <p>For safety, a memory cap is enforced via {@code MAX_BYTES}. If the request exceeds this guardrail, the original
 * request is passed through unwrapped.
 *
 * <p>Ordering is set to {@link Ordered#HIGHEST_PRECEDENCE} so the wrapper is applied before other filters.
 *
 * @see CachedBodyHttpServletRequest
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EarlyBodyCachingFilter implements Filter {
    // 2 MB guardrail; tune as needed
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    /**
     * Initializes the filter. This implementation performs no initialization.
     *
     * @param filterConfig filter configuration provided by the container
     * @throws ServletException if initialization fails (not thrown by this implementation)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    /**
     * Wraps non-GET/HEAD requests in a {@link CachedBodyHttpServletRequest} so the body can be re-read downstream. If
     * the body size exceeds the configured cap, the original request is forwarded unchanged.
     *
     * <p>This method is intentionally conservative: it leaves {@code GET} and {@code HEAD} untouched to avoid
     * unnecessary buffering of cacheable, idempotent requests.
     *
     * @param req the incoming {@link ServletRequest}
     * @param res the outgoing {@link ServletResponse}
     * @param chain the filter chain to continue processing
     * @throws IOException if reading the request body fails
     * @throws ServletException if downstream processing fails
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        ServletRequest current = req;

        if (req instanceof HttpServletRequest hreq) {
            // compute path within context
            String ctx = hreq.getContextPath();
            String uri = hreq.getRequestURI();
            String path = (ctx != null && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;

            // skip wrapping for the admin UI
            if (path != null && path.startsWith("/web")) {
                chain.doFilter(req, res);
                return;
            }

            String m = hreq.getMethod();
            if (!"GET".equalsIgnoreCase(m) && !"HEAD".equalsIgnoreCase(m)) {
                current = CachedBodyHttpServletRequest.wrap(hreq, MAX_BYTES);
            }
        }

        chain.doFilter(current, res);
    }

    /** Destroys the filter. This implementation performs no cleanup. */
    @Override
    public void destroy() {}
}
