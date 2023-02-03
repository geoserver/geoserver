/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.ows.Request;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Builder class for a {@link RuleFilter}. */
class RuleFilterBuilder {

    private Request owsRequest;
    private String ipAddress;
    private String workspace;
    private String layer;
    private Authentication user;
    private GeoFenceConfiguration config;

    private static final Logger LOGGER = Logging.getLogger(RuleFilterBuilder.class);

    RuleFilterBuilder(GeoFenceConfiguration configuration) {
        this.config = configuration;
    }

    /**
     * Set the Request object to the builder.
     *
     * @param request the OWS Request.
     * @return this builder.
     */
    RuleFilterBuilder withRequest(Request request) {
        this.owsRequest = request;
        return this;
    }

    /**
     * Set the ipAddress to the builder.
     *
     * @param ipAddress the ipAddress.
     * @return this builder.
     */
    RuleFilterBuilder withIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }

    /**
     * Set the workspace name to the builder.
     *
     * @param workspace the workspace name.
     * @return this builder.
     */
    RuleFilterBuilder withWorkspace(String workspace) {
        this.workspace = workspace;
        return this;
    }

    /**
     * Set the layer name to the builder.
     *
     * @param layer the layer name.
     * @return this builder.
     */
    RuleFilterBuilder withLayer(String layer) {
        this.layer = layer;
        return this;
    }

    /**
     * Set the authentication object to the builder.
     *
     * @param authentication the authentication object.
     * @return this builder.
     */
    RuleFilterBuilder withUser(Authentication authentication) {
        this.user = authentication;
        return this;
    }

    /**
     * Builds a {@link RuleFilter} using the values set through the various builder's method.
     *
     * @return a {@link RuleFilter} instance.
     */
    RuleFilter build() {
        RuleFilter ruleFilter = new RuleFilter(RuleFilter.SpecialFilterType.ANY);
        setRuleFilterUserAndRole(ruleFilter);
        ruleFilter.setInstance(config.getInstanceName());
        // get info from the current request
        String service = null;
        String request = null;
        if (owsRequest != null) {
            service = owsRequest.getService();
            request = owsRequest.getRequest();
        }
        if (service != null) {
            if ("*".equals(service)) {
                ruleFilter.setService(RuleFilter.SpecialFilterType.ANY);
            } else {
                ruleFilter.setService(service);
            }
        } else {
            ruleFilter.setService(RuleFilter.SpecialFilterType.DEFAULT);
        }

        if (request != null) {
            if ("*".equals(request)) {
                ruleFilter.setRequest(RuleFilter.SpecialFilterType.ANY);
            } else {
                ruleFilter.setRequest(request);
            }
        } else {
            ruleFilter.setRequest(RuleFilter.SpecialFilterType.DEFAULT);
        }
        ruleFilter.setWorkspace(workspace);
        ruleFilter.setLayer(layer);
        String sourceAddress = ipAddress;
        if (sourceAddress != null) {
            ruleFilter.setSourceAddress(sourceAddress);
        } else {
            LOGGER.log(Level.WARNING, "No source IP address found");
            ruleFilter.setSourceAddress(RuleFilter.SpecialFilterType.DEFAULT);
        }

        LOGGER.log(Level.FINE, "ResourceInfo filter: {0}", ruleFilter);

        return ruleFilter;
    }

    private void setRuleFilterUserAndRole(RuleFilter ruleFilter) {
        if (user != null) {
            setByRole(ruleFilter);
            String username = user.getName();
            if (StringUtils.isEmpty(username)) {
                LOGGER.log(Level.WARNING, "Username is null for user: {0}", new Object[] {user});
                ruleFilter.setUser(RuleFilter.SpecialFilterType.DEFAULT);
            } else {
                LOGGER.log(Level.FINE, "Setting user for filter: {0}", new Object[] {username});
                ruleFilter.setUser(username);
            }
        } else {
            LOGGER.log(Level.WARNING, "No user given");
            ruleFilter.setUser(RuleFilter.SpecialFilterType.DEFAULT);
        }
    }

    private void setByRole(RuleFilter ruleFilter) {
        // just some loggings here
        if (config.isUseRolesToFilter()) {
            if (config.getRoles().isEmpty()) {
                LOGGER.log(
                        Level.WARNING,
                        "Role filtering requested, but no roles provided. Will only use user authorizations");
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                String authList =
                        user.getAuthorities().stream()
                                .map(a -> a.getAuthority())
                                .collect(Collectors.joining(",", "[", "]"));
                LOGGER.log(
                        Level.FINE,
                        "Authorizations found for user {0}: {1}",
                        new Object[] {user.getName(), authList});

                String allowedAuth =
                        config.getRoles().stream().collect(Collectors.joining(",", "[", "]"));
                LOGGER.log(Level.FINE, "Authorizations allowed: {0}", new Object[] {allowedAuth});
            }
        }

        if (config.isUseRolesToFilter() && !config.getRoles().isEmpty()) {

            List<String> roles = getFilteredRoles();

            if (roles.isEmpty()) {
                roles.add("UNKNOWN");
            }

            String joinedRoles = String.join(",", roles);

            LOGGER.log(Level.FINE, "Setting role for filter: {0}", new Object[] {joinedRoles});
            ruleFilter.setRole(joinedRoles);
        }
    }

    List<String> getFilteredRoles() {
        boolean getAllRoles = config.getRoles().contains("*");
        Set<String> excluded =
                config.getRoles().stream()
                        .filter(r -> r.startsWith("-"))
                        .map(r -> r.substring(1))
                        .collect(Collectors.toSet());

        return getFilteredRoles(getAllRoles, excluded);
    }

    private List<String> getFilteredRoles(boolean getAllRoles, Set<String> excluded) {
        List<String> roles = new ArrayList<>();
        if (user != null) {
            for (GrantedAuthority authority : user.getAuthorities()) {
                String authRole = authority.getAuthority();
                if (addRole(authRole, excluded, getAllRoles)) roles.add(authRole);
            }
        }
        return roles;
    }

    private boolean addRole(String role, Set<String> excluded, boolean getAllRoles) {
        boolean addRole = getAllRoles || config.getRoles().contains(role);
        return addRole && !(excluded.contains(role));
    }
}
