/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import javax.xml.namespace.QName;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests covering the former functionality of GeoServerDataDirectory.
 * 
 * Much of this functionality depends on the availability of GeoServerResourceLoader
 * in the application context as the bean "resourceLoader".
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
@Category(SystemTest.class)
public class GeoserverDataDirectoryTest extends GeoServerSystemTestSupport {

    private static final char SEPARATOR_CHAR = File.separatorChar;

    private static final String RAIN_DATA_PATH = "rain" + SEPARATOR_CHAR + "rain" + SEPARATOR_CHAR
            + "rain.asc";

    private static final QName RAIN = new QName(MockData.SF_URI, "rain", MockData.SF_PREFIX);

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(RAIN, "rain.zip", "asc", getCatalog());
    }

    @Test
    public void testFindDataFile() throws IOException {
        GeoServerResourceLoader loader = getResourceLoader();
        final File file = loader.url("file:" + RAIN_DATA_PATH);
        assertNotNull(file);
    }

    @Test
    public void testFindDataFileForAbsolutePath() throws IOException {
        GeoServerResourceLoader loader = getResourceLoader();       
        final File dataDir = loader.getBaseDirectory();
        final String absolutePath = dataDir.getCanonicalPath() + SEPARATOR_CHAR + RAIN_DATA_PATH;
        final File file = loader.url(absolutePath);
        assertNotNull(file);
    }

    @Test
    public void testFindDataFileForCustomUrl() throws IOException {
        GeoServerResourceLoader loader = getResourceLoader();
        final File file = loader.url("sde://user:password@server:port");
        assertNull(file); // Before GEOS-5931 it would have been returned a file again
    }
}
