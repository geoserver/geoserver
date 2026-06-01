/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects the non-fatal problems found while loading the persisted security configuration, so that the security
 * subsystem can degrade gracefully (disabling the offending components and removing them from the filter chains)
 * instead of aborting GeoServer startup.
 *
 * <p>The typical cause is a data directory created by an older GeoServer, or by a community security plugin that is no
 * longer installed (for example {@code gs-sec-openid-connect} or {@code gs-sec-keycloak}), being opened by a GeoServer
 * that can no longer resolve the persisted filter or role service classes. Rather than throwing a hard exception, the
 * affected components are disabled and recorded here; the web UI surfaces them on the home page so an administrator can
 * migrate them manually.
 *
 * <p>The registry is rebuilt on every {@link GeoServerSecurityManager#reload() reload}.
 */
public class SecurityConfigDiagnostics {

    /** The kind of security component that was disabled. */
    public enum ComponentType {
        AUTHENTICATION_FILTER,
        AUTHENTICATION_PROVIDER,
        ROLE_SERVICE,
        USER_GROUP_SERVICE
    }

    /**
     * A security component that could not be loaded and was therefore disabled.
     *
     * @param type the kind of component
     * @param name the persisted name of the component (the data directory folder name)
     * @param alias the XStream alias / root XML element of the persisted config, when known
     * @param sourcePlugin a human readable hint about the plugin that originally created the component, when known
     *     (e.g. {@code gs-sec-openid-connect})
     * @param reason a short, human readable explanation of why the component was disabled
     */
    public record DisabledComponent(ComponentType type, String name, String alias, String sourcePlugin, String reason)
            implements Serializable {
        private static final long serialVersionUID = 1L;
    }

    /**
     * A filter chain that was altered because one or more of its filters were disabled.
     *
     * @param chainName the name of the affected request filter chain
     * @param removedFilters the names of the filters that were removed from the chain
     * @param lostAuthenticator {@code true} if the chain no longer has any authentication filter
     * @param accessDenied {@code true} if a fail-closed deny filter was injected because the chain lost its last
     *     authenticator and had no security interceptor to otherwise enforce access
     */
    public record AffectedChain(
            String chainName, List<String> removedFilters, boolean lostAuthenticator, boolean accessDenied)
            implements Serializable {
        private static final long serialVersionUID = 1L;

        public AffectedChain {
            removedFilters = List.copyOf(removedFilters);
        }
    }

    private final List<DisabledComponent> disabledComponents = new ArrayList<>();
    private final List<AffectedChain> affectedChains = new ArrayList<>();

    /** Removes all recorded diagnostics. Invoked at the start of each security configuration load. */
    public synchronized void clear() {
        disabledComponents.clear();
        affectedChains.clear();
    }

    public synchronized void addDisabledComponent(DisabledComponent component) {
        disabledComponents.add(component);
    }

    public synchronized void addAffectedChain(AffectedChain chain) {
        affectedChains.add(chain);
    }

    /** Returns an immutable snapshot of the disabled components. */
    public synchronized List<DisabledComponent> getDisabledComponents() {
        return List.copyOf(disabledComponents);
    }

    /** Returns an immutable snapshot of the affected filter chains. */
    public synchronized List<AffectedChain> getAffectedChains() {
        return List.copyOf(affectedChains);
    }

    /** {@code true} if no problems were recorded (the normal, healthy case). */
    public synchronized boolean isEmpty() {
        return disabledComponents.isEmpty() && affectedChains.isEmpty();
    }
}
