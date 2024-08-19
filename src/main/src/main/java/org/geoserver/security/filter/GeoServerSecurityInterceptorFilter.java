/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import static org.geoserver.platform.GeoServerExtensions.bean;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.WorkspaceAdminAuthorizer;
import org.geoserver.security.config.SecurityInterceptorFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.WorkspaceAdminRestAccessRule;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.util.UrlUtils;

/**
 * Security interceptor filter
 *
 * @author mcr
 * @author awaterme
 */
public class GeoServerSecurityInterceptorFilter extends GeoServerCompositeFilter {

    private static final AuthorizationDecision ACCESS_GRANTED = new AuthorizationDecision(true);
    private static final AuthorizationDecision ACCESS_DENIED = new AuthorizationDecision(false);
    private static final AuthorizationDecision ACCESS_ABSTAIN = null;

    /**
     * AuthorizationManager implementation considers the authentication requirements of the
     * respective request, in contrast to Spring's AuthenticatedAuthorizationManager, which is setup
     * for one authentication requirement explicitly.<br>
     * Based on org.springframework.security.access.vote.AuthenticatedVoter, which is deprecated
     * with Spring Security 5.8.
     */
    private static final class AuthenticatedAuthorizationManager
            implements AuthorizationManager<HttpServletRequest> {

        public static final String IS_AUTHENTICATED_FULLY = "IS_AUTHENTICATED_FULLY";

        public static final String IS_AUTHENTICATED_REMEMBERED = "IS_AUTHENTICATED_REMEMBERED";

        public static final String IS_AUTHENTICATED_ANONYMOUSLY = "IS_AUTHENTICATED_ANONYMOUSLY";

        private SecurityMetadataSource metadata;

        private AuthenticationTrustResolver authenticationTrustResolver =
                new AuthenticationTrustResolverImpl();

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
                Authentication authentication, Collection<ConfigAttribute> attributes) {
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
        public AuthorizationDecision check(
                Supplier<Authentication> authentication, HttpServletRequest request) {
            Collection<ConfigAttribute> attributes = metadata.getAttributes(request);
            return vote(authentication.get(), attributes);
        }
    }

    private static final class WorkspaceAdminAuthorizationManager
            implements AuthorizationManager<HttpServletRequest> {

        private SecurityMetadataSource metadata;

        public WorkspaceAdminAuthorizationManager(SecurityMetadataSource metadata) {
            this.metadata = metadata;
        }

        @Override
        public AuthorizationDecision check(
                Supplier<Authentication> authentication, HttpServletRequest request) {

            final Authentication auth = authentication.get();
            WorkspaceAdminAuthorizer authorizer = WorkspaceAdminAuthorizer.get();
            if (!authorizer.isFullyAuthenticated(auth)) {
                return ACCESS_DENIED;
            }

            final String uri = UrlUtils.buildRequestUrl(request);
            final HttpMethod method = HttpMethod.valueOf(request.getMethod());
            Collection<ConfigAttribute> attributes = metadata.getAttributes(request);
            boolean match =
                    attributes.stream()
                            .filter(WorkspaceAdminRestAccessRule.class::isInstance)
                            .map(WorkspaceAdminRestAccessRule.class::cast)
                            .anyMatch(rule -> rule.matches(uri, method));

            return match && authorizer.isWorkspaceAdmin(auth) ? ACCESS_GRANTED : ACCESS_ABSTAIN;
        }
    }

    /**
     * {@link AuthorizationManager} implementation considers the role requirements of the respective
     * request, in contrast to Spring's AuthorityAuthorizationManager which is setup for certain
     * roles explicitly. <br>
     * Based on org.springframework.security.access.vote.RoleVoter, which is deprecated with Spring
     * Security 5.8.
     */
    private static final class RoleAuthorizationManager
            implements AuthorizationManager<HttpServletRequest> {

        private SecurityMetadataSource metadata;

        /** @param metadata */
        public RoleAuthorizationManager(SecurityMetadataSource metadata) {
            super();
            this.metadata = metadata;
        }

        private AuthorizationDecision vote(
                Authentication authentication, Collection<ConfigAttribute> attributes) {
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
        public AuthorizationDecision check(
                Supplier<Authentication> authentication, HttpServletRequest request) {
            Collection<ConfigAttribute> attributes = metadata.getAttributes(request);
            return vote(authentication.get(), attributes);
        }
    }

    /**
     * Compound {@link AuthorizationManager} implementation forwards the check to delegates.
     *
     * <p>Simply polls all configured {@link AuthorizationManager} delegates and grants access if
     * any voted affirmatively. Denies access only if there was a deny vote AND no affirmative
     * votes.
     *
     * <p>If every {@link AuthorizationManager} delegate abstained from voting, the decision will be
     * based on the {@code allowIfAllAbstainDecisions} argument.
     *
     * @implNote Based on {@code org.springframework.security.access.vote.AffirmativeBased}, which
     *     is deprecated with Spring Security 5.8.
     */
    private static final class AffirmativeAuthorizationManager
            implements AuthorizationManager<HttpServletRequest> {

        private List<AuthorizationManager<HttpServletRequest>> delegates;
        private boolean allowIfAllAbstainDecisions;

        /**
         * @param delegates
         * @param allowIfAllAbstainDecisions
         */
        public AffirmativeAuthorizationManager(
                List<AuthorizationManager<HttpServletRequest>> delegates,
                boolean allowIfAllAbstainDecisions) {
            super();
            this.delegates = delegates;
            this.allowIfAllAbstainDecisions = allowIfAllAbstainDecisions;
        }

        @Override
        public AuthorizationDecision check(
                Supplier<Authentication> authentication, HttpServletRequest object) {
            int deny = 0;
            for (AuthorizationManager<HttpServletRequest> delegate : delegates) {
                AuthorizationDecision result = delegate.check(authentication, object);
                boolean abstain = result == ACCESS_ABSTAIN;
                if (!abstain) {
                    if (result.isGranted()) {
                        return ACCESS_GRANTED;
                    } else {
                        ++deny;
                    }
                }
            }
            if (deny > 0) {
                return ACCESS_DENIED;
            }
            // To get this far, every delegate AuthorizationManager abstained
            return allowIfAllAbstainDecisions ? ACCESS_GRANTED : ACCESS_DENIED;
        }
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        SecurityInterceptorFilterConfig siConfig = (SecurityInterceptorFilterConfig) config;
        boolean allowIfAllAbstainDecisions = siConfig.isAllowIfAllAbstainDecisions();
        String sourceName = siConfig.getSecurityMetadataSource();
        SecurityMetadataSource source = (SecurityMetadataSource) bean(sourceName);

        AuthenticatedAuthorizationManager aam = new AuthenticatedAuthorizationManager(source);
        WorkspaceAdminAuthorizationManager waam = new WorkspaceAdminAuthorizationManager(source);
        RoleAuthorizationManager ram = new RoleAuthorizationManager(source);
        AffirmativeAuthorizationManager am =
                new AffirmativeAuthorizationManager(
                        List.of(aam, waam, ram), allowIfAllAbstainDecisions);
        AuthorizationFilter filter = new AuthorizationFilter(am);

        getNestedFilters().add(filter);
    }
}
