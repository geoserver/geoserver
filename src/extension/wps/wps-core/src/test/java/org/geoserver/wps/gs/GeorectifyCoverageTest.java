/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.RenderingTransformation;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Function;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

public class GeorectifyCoverageTest extends WPSTestSupport {

    @Test
    public void testIsRenderingProcess() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Function f = ff.function("gs:GeorectifyCoverage");
        assertNotNull(f);
        assertTrue(f instanceof RenderingTransformation);
    }

    @Test
    public void testGeorectify()
            throws IOException, MismatchedDimensionException, NoSuchAuthorityCodeException,
                    FactoryException {
        GeorectifyCoverage process = applicationContext.getBean(GeorectifyCoverage.class);
        if (!process.isAvailable()) {
            LOGGER.warning("GDAL utilities are not in the path, skipping the test");
            return;
        }

        BufferedImage image = ImageIO.read(new File("./src/test/resources/rotated-image.png"));
        GridCoverage2D coverage =
                new GridCoverageFactory()
                        .create(
                                "test",
                                image,
                                new ReferencedEnvelope(
                                        0,
                                        image.getWidth(),
                                        0,
                                        image.getHeight(),
                                        CRS.decode("EPSG:404000")));
        String gcps =
                "["
                        + //
                        "[[183, 33], [-74.01183158, 40.70852996]],"
                        + //
                        "[[103, 114], [-74.01083751, 40.70754684]],"
                        + //
                        "[[459, 298], [-74.00857344, 40.71194565]],"
                        + //
                        "[[252, 139], [-74.01053024, 40.70938712]]"
                        + //
                        "]";
        Map<String, Object> map =
                process.execute(
                        coverage,
                        gcps,
                        null,
                        DefaultGeographicCRS.WGS84,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null);
        GridCoverage2D warped = (GridCoverage2D) map.get("result");
        assertEquals(CRS.decode("EPSG:4326", true), warped.getCoordinateReferenceSystem());
        // check the expected location, the output file also got verified visually
        Envelope2D envelope = warped.getEnvelope2D();
        assertEquals(-74.0122393, envelope.getMinX(), 1e-6);
        assertEquals(-74.0078822, envelope.getMaxX(), 1e-6);
        assertEquals(40.7062701, envelope.getMinY(), 1e-6);
        assertEquals(40.7126021, envelope.getMaxY(), 1e-6);
    }
}
