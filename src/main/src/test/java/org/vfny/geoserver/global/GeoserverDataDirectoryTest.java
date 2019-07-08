/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import javax.xml.namespace.QName;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resources;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests covering the former functionality of GeoServerDataDirectory.
 *
 * <p>Much of this functionality depends on the availability of GeoServerResourceLoader in the
 * application context as the bean "resourceLoader".
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
@Category(SystemTest.class)
public class GeoserverDataDirectoryTest extends GeoServerSystemTestSupport {

    private static final String EXTERNAL_ENTITIES = "externalEntities";

    private static final char SEPARATOR_CHAR = File.separatorChar;

    private static final String RAIN_DATA_PATH =
            "rain" + SEPARATOR_CHAR + "rain" + SEPARATOR_CHAR + "rain.asc";

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
        testData.addStyle(EXTERNAL_ENTITIES, "externalEntities.sld", TestData.class, getCatalog());
    }

    @Test
    public void testFindDataFile() throws IOException {
        GeoServerResourceLoader loader = getResourceLoader();
        final File file =
                Resources.find(
                        Resources.fromURL(
                                Files.asResource(loader.getBaseDirectory()),
                                "file:" + RAIN_DATA_PATH),
                        true);
        assertNotNull(file);
    }

    @Test
    public void testFindDataFileForAbsolutePath() throws IOException {
        GeoServerResourceLoader loader = getResourceLoader();
        final File dataDir = loader.getBaseDirectory();
        final String absolutePath = dataDir.getCanonicalPath() + SEPARATOR_CHAR + RAIN_DATA_PATH;
        final File file =
                Resources.find(
                        Resources.fromURL(
                                Files.asResource(loader.getBaseDirectory()), absolutePath),
                        true);
        assertNotNull(file);
    }

    @Test
    public void testFindDataFileForCustomUrl() throws IOException {
        GeoServerResourceLoader loader = getResourceLoader();
        final File file =
                Resources.find(
                        Resources.fromURL(
                                Files.asResource(loader.getBaseDirectory()),
                                "sde://user:password@server:port"),
                        true);
        assertNull(file); // Before GEOS-5931 it would have been returned a file again
    }

    @Test
    public void testStyleWithExternalEntities() throws Exception {
        GeoServerDataDirectory dd = getDataDirectory();
        StyleInfo si = getCatalog().getStyleByName(EXTERNAL_ENTITIES);
        try {
            dd.parsedStyle(si);
            fail("Should have failed with a parse error");
        } catch (Exception e) {
            String message = e.getMessage();
            assertThat(message, containsString("Entity resolution disallowed"));
            assertThat(message, containsString("/this/file/does/not/exist"));
        }
    }
}
