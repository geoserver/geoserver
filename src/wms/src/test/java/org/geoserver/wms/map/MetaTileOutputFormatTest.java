/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.*;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import org.geoserver.wms.RasterCleaner;
import org.geoserver.wms.map.QuickTileCache.MapKey;
import org.geoserver.wms.map.QuickTileCache.MetaTileKey;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.ImageWorker;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.junit.After;
import org.junit.Test;

public class MetaTileOutputFormatTest {

    MapKey mapKey = new MapKey("abcd", 0.01, new Point2D.Double(0, 0));
    MetaTileKey key =
            new MetaTileKey(
                    mapKey,
                    new Point(0, 0),
                    new ReferencedEnvelope(0, 10, 0, 10, DefaultEngineeringCRS.GENERIC_2D));
    RasterCleaner cleaner = new RasterCleaner();

    @After
    public void cleanup() {
        cleaner.finished(null);
    }

    @Test
    public void testReleaseOnBufferedImage() throws Exception {
        BufferedImage bi = new BufferedImage(768, 768, BufferedImage.TYPE_4BYTE_ABGR);
        MetatileMapOutputFormat.split(key, bi);
        assertEquals(1, cleaner.getImages().size());
    }

    @Test
    public void testReleaseOnPlanarImage() throws Exception {
        BufferedImage bi = new BufferedImage(256, 256, BufferedImage.TYPE_4BYTE_ABGR);
        RenderedImage planar =
                new ImageWorker(bi)
                        .scale(3, 3, 0, 0, Interpolation.getInstance(Interpolation.INTERP_NEAREST))
                        .getRenderedImage();
        MetatileMapOutputFormat.split(key, planar);
        assertEquals(1, cleaner.getImages().size());
    }

    @Test
    public void testPlanarImageTranslatedChild() throws Exception {
        SampleModel sm =
                new ComponentSampleModel(DataBuffer.TYPE_BYTE, 128, 128, 1, 128, new int[] {0});
        TiledImage source =
                new TiledImage(0, 0, 512, 512, 0, 0, sm, PlanarImage.createColorModel(sm));
        Raster[] tiles = source.getTiles();
        assertEquals(16, tiles.length);

        // Without fix for GEOS-8137, this split call will cause a
        // java.lang.ClassCastException: java.awt.image.Raster cannot be cast to
        // java.awt.image.WritableRaster
        MetatileMapOutputFormat.split(key, source);
    }
}
