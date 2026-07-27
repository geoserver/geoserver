/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

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

    private static final Logger LOGGER = Logging.getLogger(SecurityConfigDiagnostics.class);

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
     * <p>The {@code name}, {@code alias}, {@code sourcePlugin} and {@code reason} values originate (directly or
     * indirectly) from persisted, possibly attacker-controlled configuration in the data directory and are echoed both
     * to the server log and to the web home page. They are therefore {@link SecurityConfigDiagnostics#sanitize(String)
     * sanitized} at construction time so a crafted folder name or {@code className} cannot inject forged log lines (CR
     * / LF / control characters) nor smuggle markup downstream.
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

        public DisabledComponent {
            // sanitize at the construction boundary so every consumer (the CONFIG-level log below and the home-page
            // notice) gets the cleaned value: these strings are derived from untrusted on-disk configuration. The
            // identifier fields are short and capped tightly; the reason is internally composed human text (it may
            // legitimately be a couple of sentences) so it is only stripped of injection characters, with any
            // attacker-controlled token embedded in it already capped at the composing site.
            name = sanitize(name);
            alias = sanitize(alias);
            sourcePlugin = sanitize(sourcePlugin);
            reason = stripControlChars(reason, MAX_REASON_LENGTH);
        }
    }

    /**
     * Defends against log-forging / injection from untrusted persisted configuration: strips CR, LF and other control
     * characters (which could otherwise forge additional log lines), collapses runs of whitespace into a single space,
     * trims, and caps the length (to {@link #MAX_VALUE_LENGTH}) so a pathological value cannot bloat a log line or the
     * home-page notice. Intended for the short identifier fields (folder name, alias, plugin hint). {@code null} in,
     * {@code null} out.
     */
    static String sanitize(String value) {
        return stripControlChars(value, MAX_VALUE_LENGTH);
    }

    /**
     * Strips control characters and collapses whitespace as {@link #sanitize(String)} does, but caps to the given
     * length. Used both for the short identifier fields and, with a larger bound, for the composed reason text.
     */
    private static String stripControlChars(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        // replace any ISO control character (CR, LF, TAB, NUL, ...) with a space, then collapse whitespace runs
        String cleaned =
                value.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").trim();
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength) + "…";
        }
        return cleaned;
    }

    /** Upper bound on the length of a sanitized short identifier (folder name, alias, or plugin hint). */
    private static final int MAX_VALUE_LENGTH = 200;

    /** Upper bound on the length of a composed reason message (longer: it may span a couple of sentences). */
    private static final int MAX_REASON_LENGTH = 500;

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
        // CONFIG-level log so the migration is visible to operators running headless (no web UI to show the
        // home-page notice). One line per component disabled while tolerating a legacy / uninstalled-plugin config.
        LOGGER.log(
                Level.CONFIG,
                () -> String.format(
                        "Security migration applied: disabled %s \"%s\"%s%s - %s",
                        component.type(),
                        component.name(),
                        component.alias() != null ? " (config <" + component.alias() + ">)" : "",
                        component.sourcePlugin() != null ? " from " + component.sourcePlugin() : "",
                        component.reason()));
    }

    public synchronized void addAffectedChain(AffectedChain chain) {
        affectedChains.add(chain);
        LOGGER.log(
                Level.CONFIG,
                () -> String.format(
                        "Security migration applied: filter chain \"%s\" altered - removed %s%s%s",
                        chain.chainName(),
                        chain.removedFilters(),
                        chain.lostAuthenticator() ? "; chain lost its authenticator" : "",
                        chain.accessDenied() ? "; injected fail-closed access-denied filter" : ""));
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
