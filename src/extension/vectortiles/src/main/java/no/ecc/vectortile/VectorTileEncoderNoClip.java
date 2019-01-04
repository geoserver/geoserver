/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package no.ecc.vectortile;

import org.locationtech.jts.geom.Geometry;

/*
 * provides VectorTileEncoder that doesn't do any clipping.
 * Our clipping system is "better" (more robust, faster, and maintainable here).
 */
public class VectorTileEncoderNoClip extends VectorTileEncoder {

    public VectorTileEncoderNoClip(int extent, int polygonClipBuffer, boolean autoScale) {
        super(extent, polygonClipBuffer, autoScale);
    }

    /*
     * returns original geometry - no clipping. Assume upstream has already clipped!
     */
    protected Geometry clipGeometry(Geometry geometry) {
        return geometry;
    }
}
