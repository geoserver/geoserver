/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geoserver.util.PropertyRule;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that is possible to set a different GWC configuration directory using properties
 * GEOWEBCACHE_CONFIG_DIR_PROPERTY and GEOWEBCACHE_CACHE_DIR_PROPERTY.
 */
public final class GWCExternalConfigTest extends GeoServerSystemTestSupport {

    @Rule
    public PropertyRule configProp =
            PropertyRule.system(GeoserverXMLResourceProvider.GEOWEBCACHE_CONFIG_DIR_PROPERTY);

    @Rule
    public PropertyRule cacheProp =
            PropertyRule.system(GeoserverXMLResourceProvider.GEOWEBCACHE_CACHE_DIR_PROPERTY);

    private static final File rootTempDirectory;

    private static final String tempDirectory1;
    private static final String tempDirectory2;
    private static final String tempDirectory3;
    private static final String tempDirectory4;

    static {
        try {
            // init target directories
            rootTempDirectory = IOUtils.createTempDirectory("gwc");
            tempDirectory1 = new File(rootTempDirectory, "test-case-1").getCanonicalPath();
            tempDirectory2 = new File(rootTempDirectory, "test-case-2").getCanonicalPath();
            tempDirectory3 = new File(rootTempDirectory, "test-case-3").getCanonicalPath();
            tempDirectory4 = new File(rootTempDirectory, "test-case-4").getCanonicalPath();
        } catch (Exception exception) {
            throw new RuntimeException("Error initializing temporary directory.", exception);
        }
    }

    @Test
    public void testThatExternalDirectoryIsUsed() throws Exception {
        testUseCase(tempDirectory1, null, tempDirectory1);
        testUseCase(null, tempDirectory2, tempDirectory2);
        testUseCase(tempDirectory3, tempDirectory4, tempDirectory3);
    }

    /**
     * Helper method that setup the correct configuration variables, force Spring beans to be
     * reloaded and checks GWC configuration beans.
     */
    private void testUseCase(
            String configDirPath, String cacheDirPath, String expectedConfigFirPath) {
        // set or clear the gwc configuration directory property
        if (configDirPath == null) {
            configProp.clearValue();
        } else {
            configProp.setValue(configDirPath);
        }
        // set or clear the gwc cache directory property
        if (cacheDirPath == null) {
            cacheProp.clearValue();
        } else {
            cacheProp.setValue(cacheDirPath);
        }
        // rebuild the spring beans
        applicationContext.refresh();
        // check that the correct configuration directory is used
        applicationContext
                .getBeansOfType(GeoserverXMLResourceProvider.class)
                .values()
                .forEach(
                        bean -> {
                            try {
                                // check that configuration files are located in our custom
                                // directory
                                assertThat(bean.getConfigDirectory(), notNullValue());
                                assertThat(
                                        bean.getConfigDirectory().dir().getCanonicalPath(),
                                        is(expectedConfigFirPath));
                                // rely on canonical path for comparisons
                                assertThat(
                                        new File(bean.getLocation()).getCanonicalPath(),
                                        is(
                                                new File(
                                                                expectedConfigFirPath,
                                                                bean.getConfigFileName())
                                                        .getCanonicalPath()));
                            } catch (Exception exception) {
                                throw new RuntimeException(exception);
                            }
                        });
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        // remove the root temporary directory we created
        IOUtils.delete(rootTempDirectory);
    }
}
