/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.geoserver.security.config.SecurityAuthProviderConfig;

/**
 * XML/DTO wrapper used by the REST layer to represent a collection of {@link SecurityAuthProviderConfig} elements.
 *
 * <p>XStream is configured (in the controller) with an implicit collection on the {@code providers} field, so it
 * serializes like:
 *
 * <pre>{@code
 * <authProviders>
 *   <org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig>...</...>
 *   ...
 * </authProviders>
 * }</pre>
 *
 * <p>This class exposes a null-safe API and uses defensive copies in setters/getters while keeping a mutable field so
 * XStream can populate it directly.
 */
@XStreamAlias("authProviders")
public class AuthProviderCollection {

    /** Backing list used by XStream implicit collection. Do not make final. */
    private List<SecurityAuthProviderConfig> providers = new ArrayList<>();

    /** Required by XStream. */
    public AuthProviderCollection() {}

    /** Convenience ctor; null treated as empty. */
    public AuthProviderCollection(List<SecurityAuthProviderConfig> providers) {
        setProviders(providers);
    }

    /** Returns an unmodifiable view of the providers; never {@code null}. */
    public List<SecurityAuthProviderConfig> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    /** Replaces the collection. {@code null} becomes empty. A defensive copy is taken to prevent external mutation. */
    public void setProviders(List<SecurityAuthProviderConfig> providers) {
        this.providers = (providers == null) ? new ArrayList<>() : new ArrayList<>(providers);
    }

    /** @return the first provider or {@code null} if empty. */
    public SecurityAuthProviderConfig first() {
        return providers.isEmpty() ? null : providers.get(0);
    }

    /** @return number of providers (never negative). */
    public int size() {
        return providers.size();
    }

    /** @return {@code true} if no providers are present. */
    public boolean isEmpty() {
        return providers.isEmpty();
    }

    /** Adds a provider; no-op on {@code null}. */
    public void add(SecurityAuthProviderConfig cfg) {
        if (cfg != null) providers.add(cfg);
    }

    @Override
    public String toString() {
        return "AuthProviderCollection{providers=" + providers + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthProviderCollection)) return false;
        return Objects.equals(providers, ((AuthProviderCollection) o).providers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(providers);
    }
}
