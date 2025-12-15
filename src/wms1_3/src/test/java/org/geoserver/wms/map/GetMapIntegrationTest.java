/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.image.test.ImageAssert;
import org.junit.Test;

public class GetMapIntegrationTest extends WMSTestSupport {

    /** https://osgeo-org.atlassian.net/browse/GEOS-4893, make meta-tiler work with WMS 1.3 as well */
    @Test
    public void testMetaWMS13() throws Exception {
        String wms13 = "wms?LAYERS=cite%3ALakes&STYLES=&FORMAT=image%2Fpng&TILED=true&TILESORIGIN=-0.0018%2C0.0006"
                + "&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetMap&CRS=EPSG%3A4326&BBOX=-0.0018,0.0006,0.0007,0.0031&WIDTH=256&HEIGHT=256";

        BufferedImage image11 = ImageIO.read(getClass().getResource("lakes_meta_expected.png"));
        BufferedImage image13 = getAsImage(wms13, "image/png");

        ImageAssert.assertEquals(image11, image13, 0);
    }
}
