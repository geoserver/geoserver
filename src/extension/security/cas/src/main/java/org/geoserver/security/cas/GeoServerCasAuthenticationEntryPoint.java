/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Special CAS {@link AuthenticationEntryPoint} implementation. Clients sending requests with an
 * HTTP parameter {@link #CAS_REDIRECT} set to <code>true</code> can avoid the standard CAS
 * redirect. An unsuccessful authentication results in an HTTP 403 error. (Forbidden).
 *
 * <p>The {@link #CAS_REDIRECT} key value pair can also be sent as an HTTP requester header
 * attribute.
 *
 * @author christian
 */
public class GeoServerCasAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String CAS_REDIRECT = "casredirect";

    // private AuthenticationEntryPoint http403 = new Http403ForbiddenEntryPoint();
    private CasAuthenticationFilterConfig authConfig;

    public GeoServerCasAuthenticationEntryPoint(CasAuthenticationFilterConfig config) {
        this.authConfig = config;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {

        // check for http parameter
        String value = request.getParameter(CAS_REDIRECT);
        if (value != null && "false".equalsIgnoreCase(value)) {
            // http403.commence(request, response, authException);
            sendUnauthorized(response);
            return;
        }

        // check for header attribute
        value = request.getHeader(CAS_REDIRECT);
        if (value != null && "false".equalsIgnoreCase(value)) {
            // http403.commence(request, response, authException);
            sendUnauthorized(response);
            return;
        }

        // standard cas redirect
        ServiceProperties sp = new ServiceProperties();
        sp.setSendRenew(authConfig.isSendRenew());
        sp.setService(GeoServerCasAuthenticationFilter.retrieveService(request));

        try {
            sp.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }

        CasAuthenticationEntryPoint aep = new CasAuthenticationEntryPoint();
        aep.setLoginUrl(authConfig.getCasServerUrlPrefix() + GeoServerCasConstants.LOGIN_URI);
        aep.setServiceProperties(sp);
        try {
            aep.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }
        aep.commence(request, response, authException);
    }

    public void sendUnauthorized(ServletResponse response) throws IOException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
