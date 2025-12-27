/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import javax.imageio.ImageIO;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;

public class GetMapIntegrationTest extends WMSTestSupport {

    /** https://osgeo-org.atlassian.net/browse/GEOS-4893, make meta-tiler work with WMS 1.3 as well */
    @Test
    public void testMetaWMS13() throws Exception {
        String wms13 = "wms?LAYERS=cite%3ALakes&STYLES=&FORMAT=image%2Fpng&TILED=true&TILESORIGIN=-0.0018%2C0.0006"
                + "&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS=EPSG%3A4326&BBOX=-0.0018,0.0006,0.0007,0.0031&WIDTH=256&HEIGHT=256";

        BufferedImage image11 = ImageIO.read(getClass().getResource("lakes_meta_expected.png"));
        BufferedImage image13 = getAsImage(wms13, "image/png");
        ImageIO.write(image11, "png", new File("/tmp/wms11.png"));

        // compare the general structure
        assertEquals(image11.getWidth(), image13.getWidth());
        assertEquals(image11.getHeight(), image13.getHeight());
        assertEquals(image11.getColorModel(), image13.getColorModel());
        assertEquals(image11.getSampleModel(), image13.getSampleModel());
        // compare the actual data
        DataBufferByte db11 = (DataBufferByte) image11.getData().getDataBuffer();
        DataBufferByte db13 = (DataBufferByte) image13.getData().getDataBuffer();
        byte[][] bankData11 = db11.getBankData();
        byte[][] bankData13 = db13.getBankData();
        for (int i = 0; i < bankData11.length; i++) {
            assertArrayEquals(bankData11[i], bankData13[i]);
        }
    }
}
