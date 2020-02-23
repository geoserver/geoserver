/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.config.CredentialsFromRequestHeaderFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;

/**
 * Security filter to extract user credentials (username and password) from Request Headers. It is
 * quite flexible through the capability to extract username and password from the same or different
 * request headers, using regular expressions to capture them in a structured header content.
 *
 * @author Lorenzo Natali, GeoSolutions
 * @author Mauro Bartolomeoli, GeoSolutions
 */
public class GeoServerCredentialsFromRequestHeaderFilter extends GeoServerSecurityFilter
        implements AuthenticationCachingFilter, GeoServerAuthenticationFilter {

    private String userNameHeaderName;
    private String passwordHeaderName;
    private Pattern userNameRegex;
    private Pattern passwordRegex;
    private boolean decodeURI = true;

    private MessageDigest digest;

    protected AuthenticationEntryPoint aep;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        aep = new Http403ForbiddenEntryPoint();

        CredentialsFromRequestHeaderFilterConfig authConfig =
                (CredentialsFromRequestHeaderFilterConfig) config;

        userNameHeaderName = authConfig.getUserNameHeaderName();
        passwordHeaderName = authConfig.getPasswordHeaderName();

        userNameRegex = Pattern.compile(authConfig.getUserNameRegex());
        passwordRegex = Pattern.compile(authConfig.getPasswordRegex());
        decodeURI = authConfig.isParseAsUriComponents();

        // digest used to create a cacheKey containing the user password
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No MD5 algorithm available!");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String cacheKey = authenticateFromCache(this, (HttpServletRequest) request);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            doAuthenticate((HttpServletRequest) request, (HttpServletResponse) response);

            Authentication postAuthentication =
                    SecurityContextHolder.getContext().getAuthentication();
            if (postAuthentication != null && cacheKey != null) {
                if (cacheAuthentication(postAuthentication, (HttpServletRequest) request)) {
                    getSecurityManager()
                            .getAuthenticationCache()
                            .put(getName(), cacheKey, postAuthentication);
                }
            }
        }

        request.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        chain.doFilter(request, response);
    }

    /**
     * Parse an header string to extract the credential. The regular expression must contain a
     * group, that will represent the credential to be extracted.
     *
     * @param header the String to parse
     * @param pattern the pattern to use. This must contain one group
     */
    private String parseHeader(String header, Pattern pattern) {
        Matcher m = pattern.matcher(header);
        if (m.find() && m.groupCount() == 1) {
            String res = m.group(1);
            return res;
        } else {
            return null;
        }
    }

    /**
     * Try to authenticate. If credentials are found in the configured header(s), then
     * authentication is delegated to the AuthenticationProvider chain.
     */
    protected void doAuthenticate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String usHeader = request.getHeader(userNameHeaderName);
        String pwHeader = request.getHeader(passwordHeaderName);

        if (usHeader == null || pwHeader == null) {
            return;
        }

        String us = parseHeader(usHeader, userNameRegex);
        String pw = parseHeader(pwHeader, passwordRegex);
        if (us == null || pw == null) {
            return;
        }
        if (decodeURI) {
            us = java.net.URLDecoder.decode(us, "UTF-8");
            pw = java.net.URLDecoder.decode(pw, "UTF-8");
        }

        UsernamePasswordAuthenticationToken result =
                new UsernamePasswordAuthenticationToken(us, pw, new ArrayList<GrantedAuthority>());
        Authentication auth = null;
        try {
            auth = getSecurityManager().authenticationManager().authenticate(result);
        } catch (ProviderNotFoundException e) {
            LOGGER.log(Level.WARNING, "couldn't to authenticate user:" + us);
            return;
        }
        LOGGER.log(Level.FINER, "logged in as {0}", us);
        Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();
        for (GrantedAuthority grauth : auth.getAuthorities()) {
            roles.add((GeoServerRole) grauth);
        }
        if (!roles.contains(GeoServerRole.AUTHENTICATED_ROLE)) {
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        }

        response.addHeader("X-GeoServer-Auth-User", us);

        UsernamePasswordAuthenticationToken newResult =
                new UsernamePasswordAuthenticationToken(
                        auth.getPrincipal(), auth.getCredentials(), roles);
        newResult.setDetails(auth.getDetails());
        // Set the authentication with the roles injected
        SecurityContextHolder.getContext().setAuthentication(newResult);
    }

    @Override
    public boolean applicableForHtml() {
        return true;
    }

    @Override
    public boolean applicableForServices() {
        return true;
    }

    /** The cache key is the concatenation of the headers' values (global identifier) */
    @Override
    public String getCacheKey(HttpServletRequest req) {
        String usHeader = req.getHeader(userNameHeaderName);
        String pwHeader = req.getHeader(passwordHeaderName);
        if (usHeader == null || pwHeader == null) {
            return null;
        }

        String username = parseHeader(usHeader, userNameRegex);
        String password = parseHeader(pwHeader, passwordRegex);
        if (username == null && password == null) {
            return null;
        }
        if (decodeURI) {
            try {
                username = java.net.URLDecoder.decode(username, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                LOGGER.log(Level.WARNING, "unsupported decode user name");
            }
        }
        if (username == null || password == null) {
            return null;
        }
        StringBuffer buff = new StringBuffer(password);
        buff.append(":");
        buff.append(getName());
        String digestString = null;
        try {
            MessageDigest md = (MessageDigest) digest.clone();
            digestString = new String(Hex.encode(md.digest(buff.toString().getBytes("utf-8"))));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        buff = new StringBuffer(username);
        buff.append(":");
        buff.append(digestString);
        return buff.toString();
    }

    protected boolean cacheAuthentication(Authentication auth, HttpServletRequest request) {
        // only cache if no HTTP session is available
        if (request.getSession(false) != null) return false;

        return true;
    }
}
