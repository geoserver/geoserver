/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.mlt;

import java.awt.Rectangle;
import java.util.Set;
import org.geoserver.wms.vector.VectorTileBuilderFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;

/** Builds MapLibre Tiles (MLT) out of the WMS vector tile pipeline. */
public class MltTileBuilderFactory implements VectorTileBuilderFactory {

    public static final String MIME_TYPE = "application/vnd.maplibre-vector-tile";

    private static final Set<String> OUTPUT_FORMATS = Set.of(MIME_TYPE, "mlt");

    @Override
    public Set<String> getOutputFormats() {
        return OUTPUT_FORMATS;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    @Override
    public MltTileBuilder newBuilder(Rectangle screenSize, ReferencedEnvelope mapArea) {
        return new MltTileBuilder(screenSize);
    }

    /** MLT geometries use tile coordinates, so oversampling keeps zoom behavior stable, as for MVT. */
    @Override
    public boolean shouldOversampleScale() {
        return true;
    }

    /** Uses 16x oversampling to match the 4096 tile extent of 900913 tiles. */
    @Override
    public int getOversampleX() {
        return 16;
    }

    /** Uses 16x oversampling to match the 4096 tile extent of 900913 tiles. */
    @Override
    public int getOversampleY() {
        return 16;
    }
}
