/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class GdalTestUtil {
    private static Logger LOGGER = Logging.getLogger(GdalTestUtil.class);

    static final String TEST_RESOURCE = "/org/geoserver/data/test/tazdem.tiff";

    static final double[][] TEST_XYZ_DATA =
            new double[][] {
                {145.004166666664673, -41.004166666654271, 75},
                {145.012499999997999, -41.004166666654271, 64},
                {145.020833333331325, -41.004166666654271, 66},
                {145.029166666664679, -41.004166666654271, 52},
                {145.037499999998005, -41.004166666654271, 53}
            };

    static final int TEST_GRID_COLS = 120;
    static final double TEST_GRID_NODATA = -9999;
    static final String[] TEST_GRID_HEADER_LABEL =
            new String[] {"ncols", "nrows", "xllcorner", "yllcorner", "cellsize", "NODATA_value"};
    static final double[] TEST_GRID_HEADER_DATA =
            new double[] {
                TEST_GRID_COLS,
                240,
                144.999999999998,
                -42.999999999987,
                0.008333333333,
                TEST_GRID_NODATA
            };

    static final double EQUALS_TOLERANCE = 1E-12;

    private static Boolean IS_GDAL_AVAILABLE;
    private static String GDAL_TRANSLATE;
    private static String GDAL_DATA;

    public static boolean isGdalAvailable() {

        // check this just once
        if (IS_GDAL_AVAILABLE == null) {
            try {
                InputStream conf =
                        GdalTestUtil.class.getResourceAsStream("/gdal_translate.properties");
                Properties p = new Properties();
                if (conf != null) {
                    p.load(conf);
                }

                GDAL_TRANSLATE = p.getProperty("gdal_translate");
                // assume it's in the path if the property file hasn't been configured
                if (GDAL_TRANSLATE == null) GDAL_TRANSLATE = "gdal_translate";
                GDAL_DATA = p.getProperty("gdalData");

                GdalWrapper gdal =
                        new GdalWrapper(
                                GDAL_TRANSLATE, Collections.singletonMap("GDAL_DATA", GDAL_DATA));
                IS_GDAL_AVAILABLE = gdal.isAvailable();
            } catch (Exception e) {
                IS_GDAL_AVAILABLE = false;
                e.printStackTrace();
                LOGGER.log(
                        Level.SEVERE,
                        "Disabling gdal_translate output format tests, as gdal_translate lookup failed",
                        e);
            }
        }

        return IS_GDAL_AVAILABLE;
    }

    public static String getGdalTranslate() {
        if (isGdalAvailable()) return GDAL_TRANSLATE;
        else return null;
    }

    public static Map<String, String> getGdalData() {
        if (isGdalAvailable()) return Collections.singletonMap("GDAL_DATA", GDAL_DATA);
        else return Collections.emptyMap();
    }

    public static void checkXyzData(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        int maxCount = 5, count = 0;
        String line = null;
        while ((line = reader.readLine()) != null && count < maxCount) {
            String[] cols = line.trim().split(" ");
            assertTrue(cols.length == 3);
            assertEquals(
                    TEST_XYZ_DATA[count][0], (double) Double.valueOf(cols[0]), EQUALS_TOLERANCE);
            assertEquals(
                    TEST_XYZ_DATA[count][1], (double) Double.valueOf(cols[1]), EQUALS_TOLERANCE);
            assertEquals(
                    TEST_XYZ_DATA[count][2], (double) Double.valueOf(cols[2]), EQUALS_TOLERANCE);
            count++;
        }
    }

    public static void checkZippedGridData(InputStream input) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(input)) {
            // unzip the result
            // check contents
            boolean gridFileFound = false, auxFileFound = false, prjFileFound = false;
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().matches("^\\w+.asc.aux.xml$")) {
                    auxFileFound = true;
                    //                } else if ("tazdem.prj".equals(entry.getName())) {
                } else if (entry.getName().matches("^\\w+.prj$")) {
                    prjFileFound = true;
                    // check projection
                    checkGridProjection(zis);
                    //                } else if ("tazdem.asc".equals(entry.getName())) {
                } else if (entry.getName().matches("^\\w+.asc$")) {
                    gridFileFound = true;
                    // check grid content
                    checkGridContent(zis);
                }
            }
            assertTrue(gridFileFound);
            assertTrue(auxFileFound);
            assertTrue(prjFileFound);
        }
    }

    private static void checkGridProjection(InputStream is) throws Exception {
        String wkt = IOUtils.readLines(is).get(0);
        CoordinateReferenceSystem crs = CRS.parseWKT(wkt);
        assertNotNull(crs);
        assertEquals("GCS_WGS_1984", crs.getName().getCode());
    }

    private static void checkGridContent(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        int row = 0, maxRow = 7;
        String line = null;
        while ((line = reader.readLine()) != null && row < maxRow) {
            String[] cols = line.trim().replaceAll("\\s+", " ").split(" ");
            if (row < TEST_GRID_HEADER_LABEL.length) {
                assertEquals(2, cols.length);
                assertEquals(TEST_GRID_HEADER_LABEL[row], cols[0].trim());
                assertEquals(
                        TEST_GRID_HEADER_DATA[row],
                        Double.valueOf(cols[1].trim()),
                        EQUALS_TOLERANCE);
            } else {
                assertEquals(TEST_GRID_COLS, cols.length);
                assertEquals(75.0, Double.valueOf(cols[0].trim()), EQUALS_TOLERANCE);
            }
            row++;
        }
    }
}
