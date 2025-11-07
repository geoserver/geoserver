package org.geoserver.acl.plugin.accessmanager;

import static java.util.logging.Level.WARNING;

import com.google.common.annotations.VisibleForTesting;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.acl.authorization.AccessRequest;
import org.geoserver.acl.authorization.AdminAccessRequest;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Centralized factory for {@link AccessRequest} and {@link AdminAccessRequest} builders with shared logic for user and
 * origin IP address resolution.
 */
class AuthorizationRequestBuilder {

    public static AccessRequestBuilder data() {
        return new AccessRequestBuilder();
    }

    public static AdminAccessRequestBuilder admin() {
        return new AdminAccessRequestBuilder();
    }

    /** Builder class for an {@link AccessRequest}. */
    public static class AccessRequestBuilder {

        private String service;
        private String request;
        private Request owsRequest;

        private String ipAddress;
        private String workspace;
        private String layer;

        private Authentication user;

        private static final Logger LOGGER = Logging.getLogger(AccessRequestBuilder.class);

        AccessRequestBuilder request(@Nullable org.geoserver.ows.Request request) {
            this.owsRequest = request;
            return this;
        }

        public AccessRequestBuilder service(String service) {
            this.service = service;
            return this;
        }

        public AccessRequestBuilder request(String request) {
            this.request = request;
            return this;
        }

        public AccessRequestBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public AccessRequestBuilder workspace(String workspace) {
            this.workspace = workspace;
            return this;
        }

        public AccessRequestBuilder layer(String layer) {
            this.layer = layer;
            return this;
        }

        public AccessRequestBuilder user(Authentication authentication) {
            this.user = authentication;
            return this;
        }

        /** Builds an {@link AccessRequest} using the values set through the various builder's method. */
        public AccessRequest build() {
            Authentication auth = this.user;
            String sourceAddress = resolveSourceAddress();

            // get info from the current request
            Optional<Request> owsReq = resolveOwsRequest();
            String requestedService = resoleService(owsReq);
            String requestedServiceRequest = resolveServiceRequest(owsReq);

            String workspace = this.workspace;
            String layer = this.layer;

            AccessRequest.Builder builder = AccessRequest.builder()
                    .user(AccessRequestUserResolver.userName(auth))
                    .roles(AccessRequestUserResolver.roleNames(auth))
                    .sourceAddress(sourceAddress)
                    .service(requestedService)
                    .request(requestedServiceRequest)
                    .workspace(workspace)
                    .layer(layer);

            AccessRequest accessRequest = builder.build();
            LOGGER.log(Level.FINEST, "AccessRequest: {0}", accessRequest);

            return accessRequest;
        }

        private String resolveServiceRequest(Optional<Request> owsReq) {
            String requestedServiceRequest = this.request;
            if (requestedServiceRequest == null && owsReq.isPresent()) {
                requestedServiceRequest = owsReq.orElseThrow().getRequest();
            }
            if ("*".equals(requestedServiceRequest)) {
                requestedServiceRequest = null;
            }
            return requestedServiceRequest;
        }

        private String resoleService(Optional<Request> owsReq) {
            String requestedService = this.service;
            if (requestedService == null && owsReq.isPresent()) {
                requestedService = owsReq.orElseThrow().getService();
            }
            if ("*".equals(requestedService)) {
                requestedService = null;
            }
            return requestedService;
        }

        private Optional<Request> resolveOwsRequest() {
            return AuthorizationRequestBuilder.resolveOwsRequest(this.owsRequest);
        }

        private String resolveSourceAddress() {
            return AuthorizationRequestBuilder.resolveSourceAddress(this.ipAddress);
        }
    }

    /** Builder for {@link AdminAccessRequest} */
    public static class AdminAccessRequestBuilder {

        private String ipAddress;
        private String workspace;
        private Authentication user;

        private static final Logger LOGGER = Logging.getLogger(AdminAccessRequestBuilder.class);

        public AdminAccessRequestBuilder ipAddress(String ipAddress) {
            this.ipAddress = ipAddress;
            return this;
        }

        public AdminAccessRequestBuilder workspace(String workspace) {
            this.workspace = workspace;
            return this;
        }

        public AdminAccessRequestBuilder user(Authentication authentication) {
            this.user = authentication;
            return this;
        }

        /** Builds an {@link AdminAccessRequest} using the values set through the various builder's method. */
        public AdminAccessRequest build() {
            String sourceAddress = resolveSourceAddress();
            Authentication auth = this.user;
            String workspaceName = this.workspace;

            AdminAccessRequest accessRequest = AdminAccessRequest.builder()
                    .user(AccessRequestUserResolver.userName(auth))
                    .roles(AccessRequestUserResolver.roleNames(auth))
                    .sourceAddress(sourceAddress)
                    .workspace(workspaceName)
                    .build();

            LOGGER.log(Level.FINEST, "AdminAccessRequest: {0}", accessRequest);

            return accessRequest;
        }

        private String resolveSourceAddress() {
            return AuthorizationRequestBuilder.resolveSourceAddress(this.ipAddress);
        }
    }

    static class AccessRequestUserResolver {

        public static Set<String> roleNames(Authentication user) {
            return roleNames(user == null ? null : user.getAuthorities());
        }

        public static Set<String> roleNames(Collection<? extends GrantedAuthority> authorities) {
            if (authorities == null || authorities.isEmpty()) {
                return Set.of();
            }
            return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        }

        public static String userName(Authentication auth) {
            return auth == null ? null : auth.getName();
        }
    }

    static String resolveSourceAddress(String sourceAddress) {
        if (sourceAddress == null) {
            sourceAddress = retrieveCallerIpAddress(resolveOwsRequest());
        }
        if (sourceAddress == null) {
            AccessRequestBuilder.LOGGER.warning("No source IP address found");
        }
        return sourceAddress;
    }

    static Optional<Request> resolveOwsRequest() {
        return resolveOwsRequest(null);
    }

    static Optional<Request> resolveOwsRequest(Request defaultValue) {
        return Optional.ofNullable(defaultValue).or(() -> Optional.ofNullable(Dispatcher.REQUEST.get()));
    }

    @VisibleForTesting
    static String getSourceAddress(HttpServletRequest http) {
        if (http == null) {
            AccessRequestBuilder.LOGGER.warning("No HTTP request available.");
            return null;
        }

        String sourceAddress = null;
        try {
            final String forwardedFor = http.getHeader("X-Forwarded-For");
            final String remoteAddr = http.getRemoteAddr();
            if (forwardedFor != null) {
                String[] ips = forwardedFor.split(", ");
                sourceAddress = InetAddress.getByName(ips[0]).getHostAddress();
            } else if (remoteAddr != null) {
                // Returns an IP address, removes surrounding brackets present in case of IPV6
                // addresses
                sourceAddress = remoteAddr.replaceAll("[\\[\\]]", "");
            }
        } catch (Exception e) {
            AccessRequestBuilder.LOGGER.log(Level.INFO, "Failed to get remote address", e);
        }
        return sourceAddress;
    }

    static String retrieveCallerIpAddress(Optional<Request> owsRequest) {

        String reqSource = "Dispatcher.REQUEST";
        final HttpServletRequest request;

        // is this an OWS request
        if (owsRequest.isPresent()) {
            request = owsRequest.orElseThrow().getHttpRequest();
        } else {
            reqSource = "Spring Request";
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            request = requestAttributes == null ? null : ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        try {
            String sourceAddress = getSourceAddress(request);
            if (sourceAddress == null) {
                AccessRequestBuilder.LOGGER.log(WARNING, "Could not retrieve source address from {0}", reqSource);
            }
            return sourceAddress;
        } catch (RuntimeException ex) {
            AccessRequestBuilder.LOGGER.log(
                    WARNING, "Error retrieving source address with {0}: {1}", new Object[] {reqSource, ex.getMessage()
                    });
            return null;
        }
    }
}
