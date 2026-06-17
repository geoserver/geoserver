/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import java.util.Set;
import org.geoserver.catalog.LayerInfo;

/**
 * Signals that security restrictions changed and some cached tiles may be stale.
 *
 * <p>{@code affectedLayers}: layers to sweep, {@code null} means scope unknown so check all tile layers (the source
 * expands workspace-scoped changes to individual {@link LayerInfo}). {@code targetTags}: restrict the sweep to tiles
 * whose {@code SECURITY_TAGS_KEY} contains any of these tags, {@code null} drops all security-keyed tiles for the
 * affected layers. A tag must not contain a comma (commas separate tags in the stored value).
 */
public record SecurityConfigurationChangeEvent(Set<LayerInfo> affectedLayers, Set<String> targetTags) {
    public SecurityConfigurationChangeEvent {
        if (targetTags != null) {
            for (String tag : targetTags) {
                if (tag.indexOf(',') >= 0) {
                    throw new IllegalArgumentException("targetTag must not contain a comma: " + tag);
                }
            }
            targetTags = Set.copyOf(targetTags);
        }
    }
}
