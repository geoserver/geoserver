/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * A custom {@link HttpFirewall} implementation that allows GeoServer administrators to enable or disable the use of the
 * stricter {@link StrictHttpFirewall} through a system property. The {@link DefaultHttpFirewall} provides weaker
 * protections but may be necessary for some users who need to use certain special characters in URL paths.
 */
public class GeoServerHttpFirewall implements HttpFirewall {

    /**
     * System property to control whether or not to run requests through {@link StrictHttpFirewall}. When set to false,
     * requests will only be run through {@link DefaultHttpFirewall} which is more lenient but also more likely to allow
     * malicious requests. Default is true.
     */
    public static final String USE_STRICT_FIREWALL = "GEOSERVER_USE_STRICT_FIREWALL";

    private final DefaultHttpFirewall defaultFirewall = new DefaultHttpFirewall();

    private final StrictHttpFirewall strictFirewall = new StrictHttpFirewall();

    @Override
    public FirewalledRequest getFirewalledRequest(HttpServletRequest request) {
        // run a modified request with normalized URL paths through Spring Security's
        // StrictHttpFirewall but do not forward the normalized request to the proxy
        if (!"false".equalsIgnoreCase(GeoServerExtensions.getProperty(USE_STRICT_FIREWALL))) {
            this.strictFirewall.getFirewalledRequest(new NormalizedHttpServletRequest(request));
        }
        // use DefaultHttpFirewall here with the original request
        return this.defaultFirewall.getFirewalledRequest(request);
    }

    @Override
    public HttpServletResponse getFirewalledResponse(HttpServletResponse response) {
        // strict and default firewalls have identical behavior here
        return this.defaultFirewall.getFirewalledResponse(response);
    }

    /**
     * An {@link HttpServletRequestWrapper} that allows running a {@link HttpServletRequest} through
     * {@link StrictHttpFirewall} even when the URL path contains two consecutive slashes since there are use cases
     * where GeoServer needs to allow this type of non-normalized URL path.
     */
    private static class NormalizedHttpServletRequest extends HttpServletRequestWrapper {

        /** Regular expression for two or more consecutive forward slashes. */
        private static final Pattern FORWARD_SLASHES = Pattern.compile("//+");

        private NormalizedHttpServletRequest(HttpServletRequest request) {
            super(request);
        }

        /** Replaces consecutive forward slashes with a single slash. */
        private static String normalizeSlashes(String path) {
            return path != null ? FORWARD_SLASHES.matcher(path).replaceAll("/") : null;
        }

        @Override
        public String getContextPath() {
            return normalizeSlashes(super.getContextPath());
        }

        @Override
        public String getPathInfo() {
            return normalizeSlashes(super.getPathInfo());
        }

        @Override
        public String getRequestURI() {
            return normalizeSlashes(super.getRequestURI());
        }

        @Override
        public String getServletPath() {
            return normalizeSlashes(super.getServletPath());
        }
    }
}
