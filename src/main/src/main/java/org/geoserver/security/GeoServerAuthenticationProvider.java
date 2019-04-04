/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geotools.util.logging.Logging;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Extension of {@link AuthenticationProvider} for the geoserver security subsystem.
 *
 * <p>Instances of this class are provided by {@link GeoServerSecurityProvider}. Authentication
 * providers are configured via {@link SecurityManagerConfig#getAuthProviderNames()}.
 *
 * <p>Authentication providers are maintained by {@link GeoServerSecurityManager} in a list. During
 * authentication the manager passes an authentication request to each provider in the list until a
 * provider can successfully authenticate by returning non-null from {@link
 * #authenticate(Authentication, HttpServletRequest)}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class GeoServerAuthenticationProvider extends AbstractGeoServerSecurityService
        implements AuthenticationProvider {

    public static String DEFAULT_NAME = "default";
    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    @Override
    public final boolean supports(Class<? extends Object> authentication) {
        return supports(authentication, request());
    }

    /**
     * Same function as {@link #supports(Class)} but is provided with the current request object.
     */
    public abstract boolean supports(
            Class<? extends Object> authentication, HttpServletRequest request);

    @Override
    public final Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        return authenticate(authentication, request());
    }

    /**
     * Same function as {@link #authenticate(Authentication)} but is provided with the current
     * request object.
     *
     * <p>This method should never throw an {@link AuthenticationException}. Throwing back such an
     * exception interrupts the authentication procedure in {@link GeoServerSecurityManager} and
     * will prevent providers down the chain from processing the authentication request.
     *
     * <p>On successful authentication, this method returns an {@Link Authentication} object,
     * otherwise <code>null</code> should be returned.
     */
    public abstract Authentication authenticate(
            Authentication authentication, HttpServletRequest request);

    /** The current request. */
    HttpServletRequest request() {
        return GeoServerSecurityFilterChainProxy.REQUEST.get();
    }

    /**
     * Convenience method for logging an {@link AuthenticationException}.
     *
     * <p>This method will log the following exception types at the FINE level:
     *
     * <ul>
     *   <li>{@link UsernameNotFoundException}
     *   <li>{@link BadCredentialsException}
     *   <li>{@link DisabledException}
     * </ul>
     *
     * All other exception types are logged at WARNING.
     */
    protected void log(AuthenticationException ex) {
        Level l = Level.WARNING;
        if (ex instanceof UsernameNotFoundException
                || ex instanceof BadCredentialsException
                || ex instanceof DisabledException) {
            l = Level.FINE;
        }

        LOGGER.log(l, ex.getLocalizedMessage(), ex);
    }
}
