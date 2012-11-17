/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.impl.GeoServerUserDao;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class AuthenticationKeyManager implements ApplicationContextAware {

    static final Logger LOGGER = Logging.getLogger(AuthenticationKeyManager.class);

    AuthenticationKeyMapper mapper;

    GeoServerSecurityManager secMgr;

    public AuthenticationKeyManager(GeoServerSecurityManager secMgr) {
        this.secMgr = secMgr;
    }

    public void authenticate(String key) {
        String name = mapper.getUserName(key);
        if (name == null) {
            throw new BadCredentialsException("The authentication key " + key
                    + " is either invalid or expired");
        }

        // and then to a user, if that succeedes we build a Authentication, otherwise
        // we return an error
        //TODO: should have the filter take a reference to a user group service
        UserDetails ud = null;
        try {
            for (GeoServerUserGroupService ugs : secMgr.loadUserGroupServices()) {
                ud = ugs.loadUserByUsername(name);
                if (ud != null) {
                    break;
                }
            }
        } 
        catch(Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to load user information", e);
        }

        if (ud == null) {
            LOGGER.severe("Authentication failed, key " + key + " mapped to user " + name
                    + " which is not recognized by GeoServer");
            throw new ProviderNotFoundException("Failed to map authkey " + key
                    + " to a valid GeoServer user");
        } else {
            Authentication authentication = new KeyAuthenticationToken(key, ud);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // lookup for a plugin AuthenticationKeyMapper, if we don't find one we build the default
        // one based on a property file
        List<AuthenticationKeyMapper> mappers = GeoServerExtensions.extensions(
                AuthenticationKeyMapper.class, applicationContext);
        if (mappers.size() > 0) {
            this.mapper = mappers.get(0);
        } else {
            PropertyAuthenticationKeyMapper pam = new PropertyAuthenticationKeyMapper();
            pam.setSecurityManager(secMgr);

            this.mapper = pam;
        }
    }
}
