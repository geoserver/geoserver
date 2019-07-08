/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geoserver.authentication.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AuthUser;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.SecurityUtils;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authentication provider that delegates to GeoFence
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class GeoFenceAuthenticationProvider extends GeoServerAuthenticationProvider
        implements AuthenticationManager {

    private static final Logger LOGGER =
            Logging.getLogger(GeoFenceAuthenticationProvider.class.getName());
    // protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    private RuleReaderService ruleReaderService;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {

        LOGGER.warning("INIT FROM CONFIG");

        super.initializeFromConfig(config);
    }

    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {

        UsernamePasswordAuthenticationToken outTok = null;
        LOGGER.log(Level.FINE, "Auth request with {0}", authentication);

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken inTok =
                    (UsernamePasswordAuthenticationToken) authentication;

            AuthUser authUser = null;
            final String username = SecurityUtils.getUsername(inTok.getPrincipal());
            try {
                authUser = ruleReaderService.authorize(username, inTok.getCredentials().toString());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in authenticating with GeoFence", e);
                throw new AuthenticationException("Error in GeoFence communication", e) {};
            }

            if (authUser != null) {
                LOGGER.log(
                        Level.FINE,
                        "User {0} authenticated: {1}",
                        new Object[] {username, authUser});

                List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
                roles.addAll(inTok.getAuthorities());
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
                if (authUser.getRole() == AuthUser.Role.ADMIN) {
                    roles.add(GeoServerRole.ADMIN_ROLE);
                    roles.add(new SimpleGrantedAuthority("ADMIN")); // needed for REST?!?
                }

                outTok =
                        new UsernamePasswordAuthenticationToken(
                                username, inTok.getCredentials(), roles);

            } else { // authUser == null
                if ("admin".equals(username) && "geoserver".equals(inTok.getCredentials())) {
                    LOGGER.log(
                            Level.FINE,
                            "Default admin credentials NOT authenticated -- probably a frontend check");
                } else {
                    LOGGER.log(Level.INFO, "User {0} NOT authenticated", username);
                }
            }
            return outTok;

        } else {
            return null;
        }
    }

    public void setRuleReaderService(RuleReaderService ruleReaderService) {
        this.ruleReaderService = ruleReaderService;
    }
}
