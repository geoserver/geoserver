/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * A UserGroupService wrapper able to resolve password that have been parametrized. The placeholder
 * can be encoded in plain txt or any reversible encoding.
 */
class EnvironmentUserDetailService implements UserDetailsService {

    private GeoServerUserGroupService delegate;
    private final GeoServerEnvironment environment;

    private static Logger LOGGER = Logging.getLogger(EnvironmentUserDetailService.class);

    EnvironmentUserDetailService(
            GeoServerUserGroupService userDetailsService, GeoServerEnvironment environment) {
        this.delegate = userDetailsService;
        this.environment = environment;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails userDetails = delegate.loadUserByUsername(username);
        if (userDetails instanceof GeoServerUser) {
            GeoServerUser user = (GeoServerUser) userDetails;
            String password = user.getPassword();
            GeoServerSecurityManager manager = delegate.getSecurityManager();
            List<GeoServerPasswordEncoder> encoders =
                    manager.loadPasswordEncoders(null, true, null);
            // first decode the pwd. The placeholder might have been encoded with a reversible
            // pwd encoder
            String decodedPwd = decode(password, encoders);
            return resolvePassword(decodedPwd, user);
        }
        return userDetails;
    }

    // copy the user and set the pwd value if it was parametrized in env properties.
    // otherwise returns the user passed as a parameter.
    private GeoServerUser resolvePassword(String decodedPwd, GeoServerUser user) {
        if (isParametrized(decodedPwd)) {
            // it is a placeholder try to resolve
            String resolved = (String) environment.resolveValue(decodedPwd);
            if (!resolved.equals(decodedPwd)) {
                user = user.copy();
                user.setPassword(resolved);
            }
        }
        return user;
    }

    private boolean isParametrized(String pwd) {
        return StringUtils.isNotBlank(pwd) && pwd.startsWith("${") && pwd.endsWith("}");
    }

    private String decode(String value, List<GeoServerPasswordEncoder> encoders) {
        // doesn't directly use the pwdEncoder helper because the
        // PBE encoders needs to be init for the wrapped user group service.
        for (GeoServerPasswordEncoder encoder : encoders) {
            if (!encoder.isReversible()) continue; // should not happen
            if (encoder.isResponsibleForEncoding(value)) {
                try {
                    encoder.initializeFor(delegate);
                    return encoder.decode(value);
                } catch (IOException e) {
                    LOGGER.log(
                            Level.WARNING,
                            "Error initializing password encoder "
                                    + encoder.getName()
                                    + ", in context of "
                                    + "user pwd env resolution.",
                            e);
                }
            }
        }
        return value;
    }
}
