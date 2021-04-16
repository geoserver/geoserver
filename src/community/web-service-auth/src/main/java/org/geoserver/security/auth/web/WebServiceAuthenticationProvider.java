/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.util.IOUtils;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class WebServiceAuthenticationProvider extends GeoServerAuthenticationProvider {

    private static final String HTTP_AUTHORIZATION_HEADER = "X-HTTP-AUTHORIZATION";
    private static final String ROLE_PREFIX = "ROLE_";

    WebAuthenticationConfig config;

    @Override
    public String getName() {

        return "WebServiceAuthenticationProvider";
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {

        this.config = (WebAuthenticationConfig) config;

        verboseLog(getName() + " configured with " + config);
    }

    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        // works with user name based authentication only
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request) {
        Set<GrantedAuthority> roles = new TreeSet<>();
        roles.addAll(authentication.getAuthorities());
        String responseBody = null;
        try (CloseableHttpClient client = buildHttpClient()) {
            HttpGet get = createGetRequest(authentication);
            try (CloseableHttpResponse httpResponse = client.execute(get)) {

                int statusCode = httpResponse.getStatusLine().getStatusCode();

                if (statusCode != HttpServletResponse.SC_OK)
                    throw new AuthenticationServiceException(
                            "Web Service Authentication failed for "
                                    + authentication.getPrincipal().toString()
                                    + ". Response code is "
                                    + statusCode);

                HttpEntity entity = httpResponse.getEntity();
                responseBody = IOUtils.toString(entity.getContent());
                if (responseBody == null)
                    throw new AuthenticationServiceException(
                            "Web Service Authentication Failed for "
                                    + authentication.getPrincipal().toString());
                verboseLog("External authentication service response:" + responseBody);
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
            }
        } catch (IOException e) {
            throw new AuthenticationServiceException(
                    "Web Service Authentication Failed for "
                            + authentication.getPrincipal().toString(),
                    e);
        }

        addRoles(roles, responseBody, authentication);
        // authenticated but did find any roles..mark as anonymous
        if (roles.isEmpty()) roles.add(GeoServerRole.ANONYMOUS_ROLE);

        UsernamePasswordAuthenticationToken result =
                new UsernamePasswordAuthenticationToken(
                        authentication.getPrincipal(), authentication.getCredentials(), roles);
        if (LOGGER.isLoggable(Level.FINER)) {
            String logMessage = "user : " + authentication.getPrincipal() + "| roles: ";
            for (GrantedAuthority role : roles) {
                logMessage += role.getAuthority() + " ";
            }
            LOGGER.finer("Final Authentication:" + logMessage);
        }
        result.setDetails(authentication.getDetails());
        return result;
    }

    /* extract roles for USER from default or configured service*/
    private Set<GeoServerRole> authorize(String userName, GeoServerRoleService roleService)
            throws Exception {

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Using Role Service" + roleService.getName());

        Set<GeoServerRole> rolesFromService = roleService.getRolesForUser(userName);

        return rolesFromService;
    }

    private GeoServerRoleService getRoleService(String userName, WebAuthenticationConfig config)
            throws Exception {
        GeoServerRoleService roleService;

        if (config.getRoleServiceName() == null || config.getRoleServiceName().isEmpty())
            roleService = getSecurityManager().getActiveRoleService();
        else roleService = getSecurityManager().loadRoleService(config.getRoleServiceName());

        return roleService;
    }

    private Set<GrantedAuthority> extractRoles(final String responseBody, final String rolesRegex) {
        final Set<GrantedAuthority> authorities = new HashSet<>();
        final Pattern searchRolesRegex = Pattern.compile(rolesRegex);
        verboseLog("extracting roles using Regex:" + rolesRegex);
        Matcher matcher = searchRolesRegex.matcher(responseBody);
        if (matcher != null && matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                for (String roleName : matcher.group(i).split(",")) {
                    verboseLog("found roles :" + roleName);
                    if (!roleName.toUpperCase().startsWith(ROLE_PREFIX)) {
                        roleName = ROLE_PREFIX + roleName.toUpperCase();
                    }
                    authorities.add(new GeoServerRole(roleName.trim()));
                }
            }
        }
        return new TreeSet<>(authorities);
    }

    // returns a URL with credentials substitued in place of place holders
    private String getAuthenticationURL(String connectionURL, Authentication authentication)
            throws MalformedURLException {
        return connectionURL
                .replace(
                        WebAuthenticationConfig.URL_PLACEHOLDER_USER,
                        encode(authentication.getPrincipal().toString()))
                .replace(
                        WebAuthenticationConfig.URL_PLACEHOLDER_PASSWORD,
                        encode(authentication.getCredentials().toString()));
    }

    private void addHeaders(HttpGet httpGet, Authentication authentication) {
        String credentials =
                encode(
                        authentication.getPrincipal().toString()
                                + ":"
                                + authentication.getCredentials().toString());
        httpGet.addHeader(HTTP_AUTHORIZATION_HEADER, credentials);
    }

    private void verboseLog(String traceLog) {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer(traceLog);
        }
    }

    private Set<GrantedAuthority> checkAdminRoles(
            Set<GrantedAuthority> userRoles, GeoServerRoleService roleService) {
        // checking if role assigned to user are confgured as ADMIN or Group Admin in selected role
        // service

        final Set<GrantedAuthority> adminAuthorities = new HashSet<>();

        boolean isAdmin = false;
        boolean isGroupAdmin = false;
        for (GrantedAuthority role : userRoles) {
            GeoServerRole adminRole = roleService.getAdminRole();
            GeoServerRole groupAdminRole = roleService.getGroupAdminRole();
            if (adminRole != null && adminRole.equals(role)) isAdmin = true;
            if (groupAdminRole != null && groupAdminRole.equals(role)) isGroupAdmin = true;
        }
        // mark the role as Admin or Group Admin if any of its roles are marked as Admin or Group
        // Admin
        if (isAdmin) adminAuthorities.add(GeoServerRole.ADMIN_ROLE);
        if (isGroupAdmin) adminAuthorities.add(GeoServerRole.GROUP_ADMIN_ROLE);
        verboseLog(
                roleService.getName()
                        + ":User is Admin:"
                        + isAdmin
                        + "| User is GroupAdmin: "
                        + isGroupAdmin);
        return adminAuthorities;
    }

    private String encode(String credentials) {
        return new String(Base64.getEncoder().encode(credentials.getBytes()));
    }

    private CloseableHttpClient buildHttpClient() {
        RequestConfig clientConfig =
                RequestConfig.custom()
                        .setConnectTimeout(this.config.getConnectionTimeOut() * 1000)
                        .setSocketTimeout(this.config.getReadTimeoutOut() * 1000)
                        .build();
        return HttpClientBuilder.create().setDefaultRequestConfig(clientConfig).build();
    }

    private HttpGet createGetRequest(Authentication authentication) throws MalformedURLException {
        String authenticationURL = getAuthenticationURL(config.getConnectionURL(), authentication);
        HttpGet httpGet = new HttpGet(authenticationURL);
        if (config.isUseHeader()) addHeaders(httpGet, authentication);

        verboseLog(
                "External authentication call URL:"
                        + authenticationURL
                        + " with headers:"
                        + Stream.of(httpGet.getAllHeaders())
                                .collect(Collectors.toMap(Header::getName, Header::getValue)));
        return httpGet;
    }

    private void addRoles(
            Set<GrantedAuthority> roles, String responseBody, Authentication authentication) {
        // if a regex is set extract roles from it
        if (config.getRoleRegex() != null && !config.getRoleRegex().isEmpty()) {
            roles.addAll(extractRoles(responseBody, config.getRoleRegex()));
        }

        // next extract user roles from configured service
        // if no role service is selected, use the system default
        try {
            GeoServerRoleService roleService =
                    getRoleService(authentication.getPrincipal().toString(), config);
            roles.addAll(authorize(authentication.getPrincipal().toString(), roleService));
            roles.addAll(checkAdminRoles(roles, roleService));
        } catch (Exception e) {
            LOGGER.severe(
                    "Error getting roles from "
                            + config.getRoleServiceName()
                            + " Role Servie for user: "
                            + authentication.getPrincipal().toString());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
    }
}
