/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import com.google.common.net.InetAddresses;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * Reports the real client IP as the request remote address when the request comes through a trusted reverse proxy.
 *
 * <p>The list of trusted proxies comes from {@value #TRUSTED_PROXIES_PROPERTY}, a comma separated list of IP addresses
 * or CIDR blocks. It can be set as a system property, a web.xml context parameter, or an environment variable. When
 * unset only loopback is trusted, so a proxy on another host must be listed.
 *
 * <p>Servlet containers offer the same thing, such as Tomcat's {@code RemoteIpValve}. Doing it inside GeoServer makes
 * it work on any container, with no container setup. Unlike the valve, a loopback address in the header is never taken
 * as the client, because it names the host of the proxy that wrote it.
 *
 * <p>This filter must run before any component that reads the remote address, so it is declared first in web.xml after
 * the character encoding filter.
 */
public class RemoteIpFilter implements Filter {

    static final Logger LOGGER = Logging.getLogger(RemoteIpFilter.class);

    /** Name of the system property, context parameter or environment variable holding the trusted proxy list. */
    public static final String TRUSTED_PROXIES_PROPERTY = "GEOSERVER_TRUSTED_PROXIES";

    // this filter is the one place that reads the header, to resolve the client address once
    @SuppressWarnings("PMD.AvoidXForwardedForHeader")
    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private static final String IGNORED_HEADER_MESSAGE = "Ignoring " + FORWARDED_FOR_HEADER
            + " from {0}, not a trusted proxy. If it is the reverse proxy, add it to " + TRUSTED_PROXIES_PROPERTY;

    private static final String LOOPBACK = "127.0.0.0/8,::1/128";

    static final String DEFAULT_TRUSTED_PROXIES = LOOPBACK;

    private static final List<IpAddressMatcher> LOOPBACK_MATCHERS = buildMatchers(LOOPBACK);

    private List<IpAddressMatcher> trustedProxies;

    private final AtomicBoolean ignoredHeaderLogged = new AtomicBoolean();

    @Override
    public void init(FilterConfig config) {
        String configured = GeoServerExtensions.getProperty(TRUSTED_PROXIES_PROPERTY, config.getServletContext());
        String csv = configured == null || configured.isBlank() ? DEFAULT_TRUSTED_PROXIES : configured;
        trustedProxies = buildMatchers(csv);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest http = (HttpServletRequest) request;
            logIgnoredHeader(http);
            request = new RemoteAddrRequest(http, resolve(http, trustedProxies));
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    /** Logs once that a header from an untrusted peer was ignored, so a proxy missing from the list shows up. */
    private void logIgnoredHeader(HttpServletRequest request) {
        if (ignoredHeaderLogged.get() || request.getHeader(FORWARDED_FOR_HEADER) == null) {
            return;
        }
        String peer = request.getRemoteAddr();
        if (!matches(peer, trustedProxies) && ignoredHeaderLogged.compareAndSet(false, true)) {
            LOGGER.log(Level.WARNING, IGNORED_HEADER_MESSAGE, peer);
        }
    }

    /**
     * Returns the client IP for the request.
     *
     * <p>Honors {@code X-Forwarded-For} only when the direct peer is a trusted proxy, and then returns the right-most
     * address in the header that is not itself a trusted proxy. A port or IPv6 brackets around a hop are dropped.
     * Trusted loopback hops are skipped. A hop that is not an IP address, or an untrusted loopback hop, stops the walk
     * and the last trusted hop is returned, as the hops on its left may come from the client. Returns the peer address
     * when no trusted proxy forwarded the request or the header holds no other hop.
     */
    static String resolve(HttpServletRequest request, List<IpAddressMatcher> trusted) {
        String peer = request.getRemoteAddr();
        if (!matches(peer, trusted)) {
            return peer;
        }
        List<String> forwarded = forwardedFor(request);
        String client = peer;
        for (int i = forwarded.size() - 1; i >= 0; i--) {
            String hop = toIpAddress(forwarded.get(i));
            if (hop == null) {
                break;
            }
            boolean trustedHop = matches(hop, trusted);
            boolean loopback = matches(hop, LOOPBACK_MATCHERS);
            if (loopback && !trustedHop) {
                break;
            }
            if (loopback) {
                continue;
            }
            client = hop;
            if (!trustedHop) {
                break;
            }
        }
        return client;
    }

    /** Drops IPv6 brackets and a port from a hop, returns null when the rest is not an IP address. */
    private static String toIpAddress(String hop) {
        String ip = hop;
        int colon = ip.indexOf(':');
        if (ip.startsWith("[") && ip.indexOf(']') > 0) {
            ip = ip.substring(1, ip.indexOf(']'));
        } else if (colon > 0 && colon == ip.lastIndexOf(':')) {
            // IPv4 with a port, some proxies such as Azure Application Gateway write it
            ip = ip.substring(0, colon);
        }
        // parses literals only, never does a DNS lookup
        return InetAddresses.isInetAddress(ip) ? ip : null;
    }

    /** Collects the hops from every X-Forwarded-For header, in order, the way a proxy chain writes them. */
    private static List<String> forwardedFor(HttpServletRequest request) {
        List<String> hops = new ArrayList<>();
        Enumeration<String> values = request.getHeaders(FORWARDED_FOR_HEADER);
        while (values != null && values.hasMoreElements()) {
            for (String hop : values.nextElement().split(",")) {
                String trimmed = hop.trim();
                if (!trimmed.isEmpty()) {
                    hops.add(trimmed);
                }
            }
        }
        return hops;
    }

    private static boolean matches(String ip, List<IpAddressMatcher> matchers) {
        for (IpAddressMatcher matcher : matchers) {
            try {
                if (matcher.matches(ip)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                // ip is not a valid address, so it cannot be a trusted proxy
            }
        }
        return false;
    }

    static List<IpAddressMatcher> buildMatchers(String csv) {
        List<IpAddressMatcher> matchers = new ArrayList<>();
        for (String mask : csv.split(",")) {
            String trimmed = mask.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                matchers.add(new IpAddressMatcher(trimmed));
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Ignoring invalid trusted proxy mask: {0}", trimmed);
            }
        }
        return List.copyOf(matchers);
    }

    /** Reports the resolved client IP as the remote address, leaving the rest of the request alone. */
    private static final class RemoteAddrRequest extends HttpServletRequestWrapper {
        private final String remoteAddr;

        RemoteAddrRequest(HttpServletRequest request, String remoteAddr) {
            super(request);
            this.remoteAddr = remoteAddr;
        }

        @Override
        public String getRemoteAddr() {
            return remoteAddr;
        }

        @Override
        public String getRemoteHost() {
            return remoteAddr;
        }
    }
}
