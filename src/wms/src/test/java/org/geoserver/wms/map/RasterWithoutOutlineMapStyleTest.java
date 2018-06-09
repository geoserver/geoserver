/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Test;

/**
 * Test isCoverage function using it in a SLD style. If we have a raster layer and a LineSymbolizer
 * in the style, we can use isCoverage filter function to avoid applying the LineSymbolizer to the
 * raster.
 */
public class RasterWithoutOutlineMapStyleTest extends WMSDimensionsTestSupport {

    private static final String BASE_URL =
            "wms?service=WMS&version=1.1.0"
                    + "&request=GetMap"
                    + "&bbox=-2.237,38.562,16.593,46.558&width=200&height=80"
                    + "&srs=EPSG:4326&format=image/png";
    private static final String MIME = "image/png";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();

        testData.addStyle(
                "raster_without_outline", "../raster_without_outline.sld", getClass(), catalog);
    }

    @Test
    public void testNoOutline() throws Exception {
        BufferedImage image =
                getAsImage(BASE_URL + "&layers=watertemp&styles=raster_without_outline", MIME);

        // no black outline, so we expect white pixels
        assertPixel(image, 60, 20, new Color(255, 255, 255));

        // yellow raster
        assertPixel(image, 100, 40, new Color(255, 165, 0));

        image =
                getAsImage(
                        BASE_URL
                                + "&layers=watertemp,TimeElevation&styles=raster_without_outline,raster_without_outline",
                        MIME);

        //  black vector feature
        assertPixel(image, 24, 40, new Color(0, 0, 0));

        // no black outline, so we expect white pixels
        assertPixel(image, 60, 20, new Color(255, 255, 255));

        // yellow raster
        assertPixel(image, 100, 40, new Color(255, 165, 0));
    }
}
