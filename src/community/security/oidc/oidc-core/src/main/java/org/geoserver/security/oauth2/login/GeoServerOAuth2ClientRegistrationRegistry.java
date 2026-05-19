/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Application-wide registry of {@link ClientRegistration}s contributed by every active
 * {@link GeoServerOAuth2LoginAuthenticationFilter}.
 *
 * <h2>Why a shared registry?</h2>
 *
 * <p>Each {@code GeoServerOAuth2LoginAuthenticationFilter} internally hosts a Spring
 * {@code OAuth2AuthorizationRequestRedirectFilter}. In Spring Security 6 that filter's resolver would silently fall
 * through when a request URL named a registration ID it didn't know about, letting the next filter in the chain try. In
 * Spring Security 7 the same resolver <em>throws</em> {@code InvalidClientRegistrationIdException}, and the containing
 * {@link org.springframework.security.web.FilterChainProxy} converts the throw into HTTP 500.
 *
 * <p>If every OAuth2 filter keeps its own {@code InMemoryClientRegistrationRepository} populated only with its own
 * scoped registration IDs (e.g. {@code keycloak-test__oidc}), then the click for the second filter's button
 * ({@code /oauth2/authorization/alessio_test__oidc}) lands inside the first filter's sub-chain, the local repo does not
 * know about {@code alessio_test__oidc}, and Spring 7's resolver throws — producing the 500 Internal Server Error
 * observed in the wild when administrators configure more than one OIDC filter.
 *
 * <p>The fix is to share a single {@link ClientRegistrationRepository} instance across every
 * {@code GeoServerOAuth2LoginAuthenticationFilter}. Each filter still owns its own registrations, but Spring's resolver
 * — wherever it runs — can resolve any scoped ID into the matching {@link ClientRegistration}. Cross-filter resolution
 * is then a non-event, and the user click reaches the correct ClientRegistration regardless of which GeoServer filter
 * the request happens to traverse first.
 *
 * <h2>Lifecycle</h2>
 *
 * <p>Registry membership is keyed by GeoServer filter name. Whenever a filter is (re)built — at startup, after a save,
 * after a chain edit — the builder calls {@link #replaceFilterRegistrations(String, Collection)}, which atomically
 * replaces that filter's contribution. When a filter leaves every chain (its {@code OAuth2LoginButtonManager} sweep
 * detects it) the manager calls {@link #removeFilterRegistrations(String)} to drop the contribution from the registry.
 *
 * <p>The registry is thread-safe: read paths ({@link #findByRegistrationId(String)}, {@link #iterator()}) run lock-free
 * over a {@link ConcurrentHashMap}; write paths ({@code replace}, {@code remove}) are {@code synchronized} so that the
 * filter-keyed grouping and the flat lookup map stay consistent under concurrent filter rebuilds.
 */
public final class GeoServerOAuth2ClientRegistrationRegistry
        implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    /** Per-filter contribution: filter name → its set of registrations. */
    private final Map<String, List<ClientRegistration>> byFilter = new ConcurrentHashMap<>();

    /** Flat lookup by scoped registration ID. */
    private final Map<String, ClientRegistration> byScopedId = new ConcurrentHashMap<>();

    /**
     * Atomically replace the set of registrations contributed by a single GeoServer filter.
     *
     * <p>Calling this with an empty (or {@code null}) collection is equivalent to
     * {@link #removeFilterRegistrations(String)} — the filter's previous contribution is dropped and no new entries are
     * inserted.
     *
     * @param filterName the GeoServer filter name (e.g. {@code keycloak-test})
     * @param registrations the new contribution; may be {@code null} or empty
     */
    public synchronized void replaceFilterRegistrations(
            String filterName, Collection<ClientRegistration> registrations) {
        if (filterName == null) {
            return;
        }
        // Drop previous contribution for this filter, if any.
        List<ClientRegistration> previous = byFilter.remove(filterName);
        if (previous != null) {
            for (ClientRegistration cr : previous) {
                // Only purge from the flat map if the entry still belongs to this filter — protects against the
                // rare race where two filters claim overlapping scoped IDs.
                byScopedId.remove(cr.getRegistrationId(), cr);
            }
        }
        if (registrations == null || registrations.isEmpty()) {
            return;
        }
        List<ClientRegistration> snapshot = new ArrayList<>(registrations);
        byFilter.put(filterName, snapshot);
        for (ClientRegistration cr : snapshot) {
            byScopedId.put(cr.getRegistrationId(), cr);
        }
    }

    /**
     * Remove every registration contributed by a single GeoServer filter. Used when the filter is deleted or has been
     * dropped from every request filter chain.
     *
     * @param filterName the GeoServer filter name
     */
    public void removeFilterRegistrations(String filterName) {
        replaceFilterRegistrations(filterName, Collections.emptyList());
    }

    /**
     * Retain only the contributions for the given filter names; everything else is purged. Useful when a sweep has just
     * enumerated the currently in-chain OAuth2 filters and wants to bring the registry in line in a single pass.
     *
     * @param filterNamesToKeep the names of the filters whose contributions must be preserved
     */
    public synchronized void retainFilters(Set<String> filterNamesToKeep) {
        Set<String> toDrop = new HashSet<>(byFilter.keySet());
        toDrop.removeAll(filterNamesToKeep);
        for (String filterName : toDrop) {
            removeFilterRegistrations(filterName);
        }
    }

    /**
     * Look up a registration by its scoped ID — the primary API the Spring OAuth2 redirect / login filters call.
     *
     * @param registrationId the scoped registration ID, e.g. {@code keycloak-test__oidc}
     * @return the matching {@link ClientRegistration}, or {@code null} if no filter has contributed one
     */
    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null) {
            return null;
        }
        return byScopedId.get(registrationId);
    }

    /**
     * Iterate every currently-contributed registration. The iteration order is unspecified and the view is a snapshot —
     * concurrent contributions may or may not be reflected.
     */
    @Override
    public Iterator<ClientRegistration> iterator() {
        return new ArrayList<>(byScopedId.values()).iterator();
    }

    /** Number of registrations currently held. Useful for diagnostics and tests. */
    public int size() {
        return byScopedId.size();
    }

    /** Number of distinct filters currently contributing registrations. */
    public int filterCount() {
        return byFilter.size();
    }
}
