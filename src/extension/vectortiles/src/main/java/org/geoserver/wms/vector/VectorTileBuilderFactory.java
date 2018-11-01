/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import java.awt.Rectangle;
import java.util.Set;
import org.geotools.geometry.jts.ReferencedEnvelope;

public interface VectorTileBuilderFactory {

    /**
     * A set of identifiers for the format produced by builders from this factory. May include MIME
     * type or file extension.
     */
    Set<String> getOutputFormats();

    /** The MIME type of the format produced by builders from this factory. */
    String getMimeType();

    /**
     * Create a builder
     *
     * @param screenSize The extent of the tile in screen coordinates
     * @param mapArea The extent of the tile in target CRS coordinates
     */
    VectorTileBuilder newBuilder(Rectangle screenSize, ReferencedEnvelope mapArea);

    /**
     * Whether tiles from this builder should be "oversampled", that is, "rendered" at a higher
     * resolution than the tile resolution. The motivation for this is Mapbox vector tiles, which
     * are rendered in screen space per tile and have inconsistent behavior while zooming at lower
     * resolutions.
     *
     * @return whether this builder requires oversampling. defaults to false.
     */
    default boolean shouldOversampleScale() {
        return false;
    }

    /** @return the horizontal oversampling factor. default is 1, no oversampling */
    default int getOversampleX() {
        return 1;
    }

    /** @return the vertical oversampling factor. default is 1, no oversampling */
    default int getOversampleY() {
        return 1;
    }
}
