/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import static org.geoserver.platform.GeoServerExtensions.bean;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import org.geoserver.security.config.SecurityInterceptorFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Security interceptor filter
 *
 * @author mcr
 * @author awaterme
 */
@SuppressWarnings({"deprecation", "removal"})
public class GeoServerSecurityInterceptorFilter extends GeoServerCompositeFilter {

    private static final AuthorizationDecision ACCESS_GRANTED = new AuthorizationDecision(true);
    private static final AuthorizationDecision ACCESS_DENIED = new AuthorizationDecision(false);
    private static final AuthorizationDecision ACCESS_ABSTAIN = null;

    /**
     * AuthorizationManager implementation considers the authentication requirements of the respective request, in
     * contrast to Spring's AuthenticatedAuthorizationManager, which is setup for one authentication requirement
     * explicitly.<br>
     * Based on org.springframework.security.access.vote.AuthenticatedVoter, which is deprecated with Spring Security
     * 5.8.
     */
    private static final class AuthenticatedAuthorizationManager implements AuthorizationManager<HttpServletRequest> {

        public static final String IS_AUTHENTICATED_FULLY = "IS_AUTHENTICATED_FULLY";

        public static final String IS_AUTHENTICATED_REMEMBERED = "IS_AUTHENTICATED_REMEMBERED";

        public static final String IS_AUTHENTICATED_ANONYMOUSLY = "IS_AUTHENTICATED_ANONYMOUSLY";

        private SecurityMetadataSource metadata;

        private AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();

        /** @param metadata */
        public AuthenticatedAuthorizationManager(SecurityMetadataSource metadata) {
            super();
            this.metadata = metadata;
        }

        private boolean isFullyAuthenticated(Authentication authentication) {
            return (!this.authenticationTrustResolver.isAnonymous(authentication)
                    && !this.authenticationTrustResolver.isRememberMe(authentication));
        }

        private boolean supports(ConfigAttribute attribute) {
            return (attribute.getAttribute() != null)
                    && (IS_AUTHENTICATED_FULLY.equals(attribute.getAttribute())
                            || IS_AUTHENTICATED_REMEMBERED.equals(attribute.getAttribute())
                            || IS_AUTHENTICATED_ANONYMOUSLY.equals(attribute.getAttribute()));
        }

        private AuthorizationDecision vote(
                Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
            AuthorizationDecision result = ACCESS_ABSTAIN;
            for (ConfigAttribute attribute : attributes) {
                if (this.supports(attribute)) {
                    result = ACCESS_DENIED;
                    if (IS_AUTHENTICATED_FULLY.equals(attribute.getAttribute())) {
                        if (isFullyAuthenticated(authentication)) {
                            return ACCESS_GRANTED;
                        }
                    }
                    if (IS_AUTHENTICATED_REMEMBERED.equals(attribute.getAttribute())) {
                        if (this.authenticationTrustResolver.isRememberMe(authentication)
                                || isFullyAuthenticated(authentication)) {
                            return ACCESS_GRANTED;
                        }
                    }
                    if (IS_AUTHENTICATED_ANONYMOUSLY.equals(attribute.getAttribute())) {
                        if (this.authenticationTrustResolver.isAnonymous(authentication)
                                || isFullyAuthenticated(authentication)
                                || this.authenticationTrustResolver.isRememberMe(authentication)) {
                            return ACCESS_GRANTED;
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public AuthorizationDecision check(Supplier<Authentication> authentication, HttpServletRequest request) {
            Collection<ConfigAttribute> attributes =
                    Optional.ofNullable(metadata.getAttributes(request)).orElse(Collections.emptySet());
            return vote(authentication.get(), request, attributes);
        }
    }

    /**
     * {@link AuthorizationManager} implementation considers the role requirements of the respective request, in
     * contrast to Spring's AuthorityAuthorizationManager which is setup for certain roles explicitly. <br>
     * Based on org.springframework.security.access.vote.RoleVoter, which is deprecated with Spring Security 5.8.
     */
    private static final class RoleAuthorizationManager implements AuthorizationManager<HttpServletRequest> {

        private SecurityMetadataSource metadata;

        /** @param metadata */
        public RoleAuthorizationManager(SecurityMetadataSource metadata) {
            super();
            this.metadata = metadata;
        }

        private AuthorizationDecision vote(
                Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
            if (authentication == null) {
                return ACCESS_DENIED;
            }
            AuthorizationDecision result = ACCESS_ABSTAIN;
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            for (ConfigAttribute attribute : attributes) {
                if (attribute.getAttribute() != null) {
                    result = ACCESS_DENIED;
                    // Attempt to find a matching granted authority
                    for (GrantedAuthority authority : authorities) {
                        if (attribute.getAttribute().equals(authority.getAuthority())) {
                            return ACCESS_GRANTED;
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public AuthorizationDecision check(Supplier<Authentication> authentication, HttpServletRequest request) {
            Collection<ConfigAttribute> attributes =
                    Optional.ofNullable(metadata.getAttributes(request)).orElse(Collections.emptySet());
            return vote(authentication.get(), request, attributes);
        }
    }

    /**
     * Compound {@link AuthorizationManager} implementation forwards the check to delegates. <br>
     * Based on org.springframework.security.access.vote.AffirmativeBased, which is deprecated with Spring Security 5.8.
     */
    private static final class AffirmativeAuthorizationManager implements AuthorizationManager<HttpServletRequest> {

        private AuthorizationManager<HttpServletRequest> delegate1;
        private AuthorizationManager<HttpServletRequest> delegate2;
        private boolean allowIfAllAbstainDecisions;

        /**
         * @param delegate1
         * @param delegate2
         * @param allowIfAllAbstainDecisions
         */
        public AffirmativeAuthorizationManager(
                AuthorizationManager<HttpServletRequest> delegate1,
                AuthorizationManager<HttpServletRequest> delegate2,
                boolean allowIfAllAbstainDecisions) {
            super();
            this.delegate1 = delegate1;
            this.delegate2 = delegate2;
            this.allowIfAllAbstainDecisions = allowIfAllAbstainDecisions;
        }

        @Override
        public AuthorizationDecision check(Supplier<Authentication> authentication, HttpServletRequest object) {
            AuthorizationDecision d1 = delegate1.check(authentication, object);
            AuthorizationDecision d2 = delegate2.check(authentication, object);
            if (d1 == null && d2 == null) {
                return allowIfAllAbstainDecisions ? ACCESS_GRANTED : ACCESS_DENIED;
            }
            if (d1 != null && d1.isGranted()) {
                return ACCESS_GRANTED;
            }
            if (d2 != null && d2.isGranted()) {
                return ACCESS_GRANTED;
            }
            return ACCESS_DENIED;
        }
    }

    public void initializeFromConfig(SecurityNamedServiceConfig config, SecurityMetadataSource source)
            throws IOException {
        super.initializeFromConfig(config);

        SecurityInterceptorFilterConfig siConfig = (SecurityInterceptorFilterConfig) config;
        boolean allowIfAllAbstainDecisions = siConfig.isAllowIfAllAbstainDecisions();

        AuthenticatedAuthorizationManager aam = new AuthenticatedAuthorizationManager(source);
        RoleAuthorizationManager ram = new RoleAuthorizationManager(source);
        AffirmativeAuthorizationManager am = new AffirmativeAuthorizationManager(aam, ram, allowIfAllAbstainDecisions);
        AuthorizationFilter filter = new AuthorizationFilter(am);

        getNestedFilters().add(filter);
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        SecurityInterceptorFilterConfig siConfig = (SecurityInterceptorFilterConfig) config;
        String sourceName = siConfig.getSecurityMetadataSource();
        SecurityMetadataSource source = (SecurityMetadataSource) bean(sourceName);
        initializeFromConfig(config, source);
    }
}
