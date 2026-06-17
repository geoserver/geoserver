/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import java.io.Serial;
import java.util.List;
import javax.annotation.Nullable;
import org.geowebcache.filter.parameters.ParameterFilter;

/**
 * A GWC {@link ParameterFilter} that injects the security cache key as a tile dimension, ensuring that tiles cached
 * under different access restrictions are stored separately.
 *
 * <p>The key value is computed by {@link org.geoserver.gwc.layer.GeoServerTileLayer#getModifiableParameters} and
 * injected into the tile parameter map. This filter simply passes the value through unchanged.
 *
 * <p><strong>Warning:</strong> the {@link #ACCESS_LIMITS_KEY} and {@link #SECURITY_TAGS_KEY} constants are not
 * persisted in the GWC configuration, but they are saved in the parameter filters property file for each tile cache.
 * Changing them orphans all existing tile caches.
 */
public class SecurityParameterFilter extends ParameterFilter {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Parameter key for the security access-limits cache dimension. */
    public static final String ACCESS_LIMITS_KEY = "ACCESS_LIMITS_KEY";

    /** Parameter key for the security-tags cache dimension, used for targeted cache invalidation. */
    public static final String SECURITY_TAGS_KEY = "SECURITY_TAGS_KEY";

    public SecurityParameterFilter(String key) {
        super(key, ""); // "" = unrestricted access (no security key)
    }

    @Override
    protected Object readResolve() {
        // synthetic filter - must never appear in persisted config; security would silently
        // break if deserialized because the filter would survive even after security is disabled
        throw new UnsupportedOperationException(
                "SecurityParameterFilter must not be persisted; it is added only at runtime");
    }

    /** Synthetic cache-partitioning filter: never advertised in preview, seed form or WMS/WMTS capabilities. */
    @Override
    public boolean isUserVisible() {
        return false;
    }

    /**
     * Returns the value unchanged. The security key is computed upstream; this filter exists only to register the
     * parameter as a GWC cache dimension.
     */
    @Override
    public String apply(@Nullable String value) {
        return value == null ? getDefaultValue() : value;
    }

    /** Returns {@code null}: the set of possible security keys is unbounded. */
    @Override
    public @Nullable List<String> getLegalValues() {
        return null;
    }
}
