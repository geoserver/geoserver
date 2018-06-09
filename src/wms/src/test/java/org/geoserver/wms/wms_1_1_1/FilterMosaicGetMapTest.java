/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.geoserver.wms.WMSFilterMosaicTestSupport;

/**
 * Class to test ImageMosaic cql filter
 *
 * @see {@link WMSFilterMosaicTestSupport}
 * @author carlo cancellieri
 */
public class FilterMosaicGetMapTest extends WMSFilterMosaicTestSupport {

    static final String layer = WATTEMP.getLocalPart();

    static final String BASE_URL =
            "wms?service=WMS&version=1.1.0"
                    + "&request=GetMap&layers="
                    + layer
                    + "&styles="
                    + "&bbox=0.237,40.562,14.593,44.558&width=200&height=80"
                    + "&srs=EPSG:4326&format=image/png";

    static final String MIME = "image/png";

    // specifying default filter
    static final String cql_filter = "elevation=100 AND ingestion=\'2008-10-31T00:00:00.000Z\'";

    public void testAsCQL() throws Exception {
        // CASE 'MOSAIC WITH DEFAULT FILTERS'

        // setting the default filter
        super.setupMosaicFilter(cql_filter, layer);

        // get mosaic using the default filter
        BufferedImage image = getAsImage(BASE_URL, "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(0, 0, 0));
        assertPixel(image, 68, 72, new Color(240, 240, 255));
    }

    public void testCaseDefault() throws Exception {
        // CASE 'MOSAIC WITHOUT FILTERS'

        // disable the default filter
        super.setupMosaicFilter("", layer);

        // get mosaic without the default filter
        BufferedImage image = getAsImage(BASE_URL, "image/png");

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 182, 182));
    }

    public void testCaseElev100andIngestion31Oct() throws Exception {
        // CASE 'MOSAIC WITH FILTERS'

        // overriding the default filter using cql_filter parameter
        BufferedImage image =
                getAsImage(
                        BASE_URL
                                + "&cql_filter=elevation=100 AND ingestion=\'2008-10-31T00:00:00.000Z\'",
                        "image/png");

        // setting the default filter
        super.setupMosaicFilter(cql_filter, layer);

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(0, 0, 0));
        assertPixel(image, 68, 72, new Color(240, 240, 255));
    }

    public void testCaseElev100andIngestion01Nov() throws Exception {

        // CASE 'MOSAIC WITH FILTERS'

        // overriding the default filter using cql_filter parameter
        BufferedImage image =
                getAsImage(
                        BASE_URL
                                + "&cql_filter=elevation=100 AND ingestion=\'2008-11-01T00:00:00.000Z\'",
                        "image/png");

        // setting the default filter
        super.setupMosaicFilter(cql_filter, layer);

        // at this elevation the pixel is black
        assertPixel(image, 36, 31, new Color(0, 0, 0));
        assertPixel(image, 68, 72, new Color(246, 246, 255));
    }

    public void testCaseElev0andIngestion31Oct() throws Exception {
        // CASE 'MOSAIC WITH FILTERS'

        // overriding the default filter using cql_filter parameter
        BufferedImage image =
                getAsImage(
                        BASE_URL
                                + "&cql_filter=elevation=0 AND ingestion=\'2008-10-31T00:00:00.000Z\'",
                        "image/png");

        // setting the default filter
        super.setupMosaicFilter(cql_filter, layer);

        // should be similar to the default, but with different shades of color
        assertPixel(image, 36, 31, new Color(246, 246, 255));
        assertPixel(image, 68, 72, new Color(255, 182, 182));
    }

    public void testCaseElev0andIngestion01Nov() throws Exception {
        // CASE 'MOSAIC WITH FILTERS'

        // overriding the default filter using cql_filter parameter
        BufferedImage image =
                getAsImage(
                        BASE_URL
                                + "&cql_filter=elevation=0 AND ingestion=\'2008-11-01T00:00:00.000Z\'",
                        "image/png");

        // setting the default filter
        super.setupMosaicFilter(cql_filter, layer);

        assertPixel(image, 36, 31, new Color(246, 246, 255));
        // and this one a light blue, but slightly darker than before
        assertPixel(image, 68, 72, new Color(255, 185, 185));
    }
}
