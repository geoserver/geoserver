/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2010 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jai;

import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import javax.media.jai.RasterFactory;
import javax.media.jai.TileFactory;
import javax.media.jai.TileRecycler;

/**
 * A pass-through recycling tile factory that does not cache tiles but simply creates new ones as
 * asked
 *
 * <p>It is crucial to avoid using the standard one when we don't want caching as it might pop-up
 * here and there otherwise.
 *
 * @author Simone Giannecchini - GeoSolutions
 */
class PassThroughTileFactory implements TileFactory, TileRecycler {

    /** Constructs a <code>RecyclingTileFactory</code>. */
    public PassThroughTileFactory() {}

    /** Returns <code>false</code> since we don't cache. */
    public boolean canReclaimMemory() {
        return false;
    }

    /** Returns <code>false</code> since we don't cache. */
    public boolean isMemoryCache() {
        return false;
    }

    /** Always returns -1, does not do used memory accounting */
    public long getMemoryUsed() {
        return -1;
    }

    /** Clean up the cache. Noop. */
    public void flush() {}

    /** Builds a new tile, creating it from scratch. */
    public WritableRaster createTile(SampleModel sampleModel, Point location) {
        // sanity checks
        if (sampleModel == null) {
            throw new NullPointerException("sampleModel cannot be null");
        }
        if (location == null) {
            location = new Point(0, 0);
        }

        return RasterFactory.createWritableRaster(sampleModel, location);
    }

    /** Recycles the given tile. Noop. */
    public void recycleTile(Raster tile) {}
}
