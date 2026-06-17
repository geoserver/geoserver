/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.geoserver.security.ResourceAccessManager;
import org.geotools.api.parameter.GeneralParameterDescriptor;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.util.logging.Logging;

/**
 * Tracks {@link GeneralParameterValue} descriptors that are safe to ignore when building a security cache key - i.e.
 * parameters that do not affect tile content.
 *
 * <p>Three registration mechanisms, checked in order:
 *
 * <ol>
 *   <li>Built-in descriptors, see ({@link IgnorableParameterRegistry#BUILT_INS}
 *   <li>Programmatic registration via {@link #registerIgnorable(GeneralParameterDescriptor)}
 *   <li>System property {@value #SYSTEM_PROPERTY} (comma-separated descriptor codes) - operator last resort when the
 *       {@link ResourceAccessManager} cannot be modified.
 * </ol>
 */
public class IgnorableParameterRegistry {

    static final String SYSTEM_PROPERTY = "gwc.security.params.ignorable";

    private static final Logger LOGGER = Logging.getLogger(IgnorableParameterRegistry.class);

    // common well-known ignorable parameters
    private static final Set<GeneralParameterDescriptor> BUILT_INS = Set.of(
            AbstractGridFormat.USE_IMAGEN_IMAGEREAD,
            ImageMosaicFormat.ALLOW_MULTITHREADING,
            ImageMosaicFormat.MAX_ALLOWED_TILES,
            AbstractGridFormat.SUGGESTED_TILE_SIZE);

    private final Set<GeneralParameterDescriptor> contributed = ConcurrentHashMap.newKeySet();
    private final Set<String> extraNames;

    public IgnorableParameterRegistry() {
        String extra = System.getProperty(SYSTEM_PROPERTY);
        if (extra != null) {
            Set<String> names = new TreeSet<>();
            for (String name : extra.split("\\s*,\\s*")) {
                if (!name.isEmpty()) {
                    names.add(name);
                    LOGGER.config("GWC security: treating parameter '" + name + "' as ignorable (via " + SYSTEM_PROPERTY
                            + ")");
                }
            }
            extraNames = Collections.unmodifiableSet(names);
        } else {
            extraNames = Set.of();
        }
    }

    /**
     * Registers a descriptor whose parameter values do not affect tile content. Call this during application startup
     * (e.g. in {@code afterPropertiesSet}) before any tile requests are served.
     */
    public void registerIgnorable(GeneralParameterDescriptor descriptor) {
        contributed.add(descriptor);
    }

    /** Returns {@code true} if this parameter does not affect tile content and can be skipped during key building. */
    public boolean isIgnorable(GeneralParameterValue param) {
        GeneralParameterDescriptor descriptor = param.getDescriptor();
        return BUILT_INS.contains(descriptor)
                || contributed.contains(descriptor)
                || extraNames.contains(descriptor.getName().getCode());
    }
}
