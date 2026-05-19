/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.user;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

/** GeoServer user principal that also exposes Spring OAuth2 user attributes. */
public class GeoServerOAuth2User extends GeoServerUser implements OAuth2User {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> attributes;
    private final String nameAttributeKey;

    /**
     * Creates a GeoServer-backed OAuth2 principal.
     *
     * @param username resolved principal name
     * @param authorities resolved GeoServer authorities
     * @param properties resolved GeoServer user properties
     * @param attributes OAuth2 attributes exposed by the upstream provider
     * @param nameAttributeKey provider attribute used to derive the Spring principal name
     */
    public GeoServerOAuth2User(
            String username,
            Collection<GeoServerRole> authorities,
            Properties properties,
            Map<String, Object> attributes,
            String nameAttributeKey) {
        super(username);
        setAuthorities(authorities == null ? Collections.emptySet() : Set.copyOf(authorities));
        getProperties().putAll(properties == null ? new Properties() : properties);
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes == null ? Map.of() : attributes));
        this.nameAttributeKey = nameAttributeKey;
    }

    /** Returns the Spring-style principal name, falling back to the GeoServer username when absent. */
    @Override
    public String getName() {
        Object name = nameAttributeKey == null ? null : attributes.get(nameAttributeKey);
        return name instanceof String s && !s.isBlank() ? s : getUsername();
    }

    /** Returns the immutable OAuth2 attribute map exposed by the provider. */
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
