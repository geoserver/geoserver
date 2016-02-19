/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.*;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import javax.media.jai.Interpolation;

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
    MetaTileKey key = new MetaTileKey(mapKey, new Point(0, 0), new ReferencedEnvelope(0, 10, 0, 10, DefaultEngineeringCRS.GENERIC_2D));
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
        RenderedImage planar = new ImageWorker(bi).scale(3, 3, 0, 0, Interpolation.getInstance(Interpolation.INTERP_NEAREST)).getRenderedImage();
        MetatileMapOutputFormat.split(key, planar);
        assertEquals(1, cleaner.getImages().size());
    }
}
