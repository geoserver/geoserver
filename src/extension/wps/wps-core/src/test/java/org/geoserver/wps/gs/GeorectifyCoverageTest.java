/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.imageio.ImageIO;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.NoSuchAuthorityCodeException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.RenderingTransformation;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GeorectifyCoverageTest extends WPSTestSupport {

    private static final String GDAL_CONFIG = "gdalops.properties";

    @Test
    public void testIsRenderingProcess() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Function f = ff.function("gs:GeorectifyCoverage");
        assertNotNull(f);
        assertTrue(f instanceof RenderingTransformation);
    }

    @Rule
    public TemporaryFolder folders = new TemporaryFolder();

    @After
    public void removeConfigFile() {
        getResourceLoader().get(GDAL_CONFIG).delete();
    }

    /** Writes the given lines to the GDAL configuration file and returns a configuration reading it */
    private GeorectifyConfiguration configure(String... lines) throws IOException {
        try (OutputStream out = getResourceLoader().get(GDAL_CONFIG).out()) {
            out.write(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        }
        return new GeorectifyConfiguration();
    }

    /** Properties files use the backslash as an escape, make the path safe to write in one */
    private String propertyPath(File folder) {
        return folder.getAbsolutePath().replace("\\", "/");
    }

    @Test
    public void testConfigurationReadsOnlyKnownKeys() throws IOException {
        GeorectifyConfiguration config =
                configure("GDAL_CACHEMAX=16000000", "GDAL_TRANSLATE_PARAMS=-expand rgb", "GDAL_EXTRA_PATH=/opt/gdal");

        // recognized keys are applied
        assertEquals("-expand rgb", config.getGdalTranslateParameters());
        // only the known environment settings reach the environment, unknown keys are dropped
        Map<String, String> env = config.getEnvVariables();
        assertEquals(1, env.size());
        assertEquals("16000000", env.get("GDAL_CACHEMAX"));
    }

    @Test
    public void testFolderVariables() throws IOException {
        File data = folders.newFolder("gdal-data");
        File logging = folders.newFolder("gdal-logging");
        File temp = folders.newFolder("gdal-temp");

        GeorectifyConfiguration config = configure(
                "GDAL_DATA=" + propertyPath(data),
                "GDAL_LOGGING_DIR=" + propertyPath(logging),
                "TEMP_DIR=" + propertyPath(temp));

        Map<String, String> env = config.getEnvVariables();
        assertEquals(3, env.size());
        assertEquals(propertyPath(data), env.get("GDAL_DATA"));
        assertEquals(propertyPath(logging), env.get("GDAL_LOGGING_DIR"));
        assertEquals(propertyPath(temp), env.get("TEMP_DIR"));
    }

    @Test
    public void testMissingFolderVariable() throws IOException {
        File missing = new File(folders.getRoot(), "not-there");
        File file = folders.newFile("plain-file");

        GeorectifyConfiguration config =
                configure("TEMP_DIR=" + propertyPath(missing), "GDAL_DATA=" + propertyPath(file));

        // a missing folder and a plain file are both rejected
        assertEquals(0, config.getEnvVariables().size());
    }

    @Test
    public void testGeorectify()
            throws IOException, MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException {
        GeorectifyCoverage process = applicationContext.getBean(GeorectifyCoverage.class);
        if (!process.isAvailable()) {
            LOGGER.warning("GDAL utilities are not in the path, skipping the test");
            return;
        }

        BufferedImage image = ImageIO.read(new File("./src/test/resources/rotated-image.png"));
        GridCoverage2D coverage = new GridCoverageFactory()
                .create(
                        "test",
                        image,
                        new ReferencedEnvelope(0, image.getWidth(), 0, image.getHeight(), CRS.decode("EPSG:404000")));
        String gcps = "["
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
                process.execute(coverage, gcps, null, DefaultGeographicCRS.WGS84, null, null, null, false, null, null);
        GridCoverage2D warped = (GridCoverage2D) map.get("result");
        assertEquals(CRS.decode("EPSG:4326", true), warped.getCoordinateReferenceSystem());
        // check the expected location, the output file also got verified visually
        ReferencedEnvelope envelope = warped.getEnvelope2D();
        assertEquals(-74.0122393, envelope.getMinX(), 1e-4);
        assertEquals(-74.0078822, envelope.getMaxX(), 1e-4);
        assertEquals(40.7062701, envelope.getMinY(), 1e-4);
        assertEquals(40.7126021, envelope.getMaxY(), 1e-4);
    }
}
