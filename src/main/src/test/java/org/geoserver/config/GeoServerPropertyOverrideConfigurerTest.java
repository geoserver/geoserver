/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.easymock.EasyMock;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.Assume;
import org.junit.Test;

public class GeoServerPropertyOverrideConfigurerTest {

    @Test
    public void testPropertyOverrider() {
        // on easymock 3.6 + jdk11 this test does not work, waiting for 3.7. to be released
        Assume.assumeFalse(SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9));

        // corner cases
        testPropertyOverride("", "", "");
        testPropertyOverride("some text", "data dir", "some text");
        testPropertyOverride("some ${GEOSERVER_DATA_DIR} text", "", "some  text");
        testPropertyOverride("some ${GEOSERVER_DATA_DIR} text", "\\$/", "some \\$/ text");

        // linux paths
        testPropertyOverride("before/${GEOSERVER_DATA_DIR}/after", "", "before//after");
        testPropertyOverride("${GEOSERVER_DATA_DIR}", "/linux/path/", "/linux/path/");
        testPropertyOverride(
                "before/${GEOSERVER_DATA_DIR}/after", "linux/path", "before/linux/path/after");
        testPropertyOverride(
                "before/a space/${GEOSERVER_DATA_DIR}/after/another space",
                "linux/path",
                "before/a space/linux/path/after/another space");
        testPropertyOverride(
                "before/a space/${GEOSERVER_DATA_DIR}/after/another space",
                "linux/a space/path",
                "before/a space/linux/a space/path/after/another space");

        // windows paths
        testPropertyOverride("before\\${GEOSERVER_DATA_DIR}\\after", "", "before\\\\after");
        testPropertyOverride("${GEOSERVER_DATA_DIR}", "\\linux\\path\\", "\\linux\\path\\");
        testPropertyOverride(
                "before\\${GEOSERVER_DATA_DIR}\\after",
                "linux\\path",
                "before\\linux\\path\\after");
        testPropertyOverride(
                "before\\a space\\${GEOSERVER_DATA_DIR}\\after\\another space",
                "linux\\path",
                "before\\a space\\linux\\path\\after\\another space");
        testPropertyOverride(
                "before\\a space\\${GEOSERVER_DATA_DIR}\\after\\another space",
                "linux\\a space\\path",
                "before\\a space\\linux\\a space\\path\\after\\another space");

        // non ascii paths
        testPropertyOverride(
                "/Entit\u00E9G\u00E9n\u00E9rique/${GEOSERVER_DATA_DIR}/\u901A\u7528\u5B9E\u4F53",
                "some\u00E4/\u00DFtext",
                "/Entit\u00E9G\u00E9n\u00E9rique/some\u00E4/\u00DFtext/\u901A\u7528\u5B9E\u4F53");
        testPropertyOverride(
                "\\Entit\u00E9G\u00E9n\u00E9rique\\${GEOSERVER_DATA_DIR}\\\u901A\u7528\u5B9E\u4F53",
                "some\u00E4\\\u00DFtext",
                "\\Entit\u00E9G\u00E9n\u00E9rique\\some\u00E4\\\u00DFtext\\\u901A\u7528\u5B9E\u4F53");
    }

    // Helper method that test that a GEOSERVER_DATA_DIR placeholder is correctly overridden by a
    // specific path
    private void testPropertyOverride(
            String property, String dataDirectoryPath, String expectedResult) {
        GeoServerPropertyOverrideConfigurer overrider = getOverriderForPath(dataDirectoryPath);
        String result = overrider.convertPropertyValue(property);
        if (expectedResult == null) {
            assertThat(result, nullValue());
        } else {
            assertThat(result, notNullValue());
            assertThat(result, is(expectedResult));
        }
    }

    // Helper method that creates an overrider instance that will use the specified data directory
    // path
    private GeoServerPropertyOverrideConfigurer getOverriderForPath(String dataDirectoryPath) {
        GeoServerDataDirectory dataDirectory = createGeoServerDataDirectoryMock(dataDirectoryPath);
        return new GeoServerPropertyOverrideConfigurer(dataDirectory);
    }

    // Helper method that creates a mocked GeoServer data directory allowing us to use a specific
    // path
    private GeoServerDataDirectory createGeoServerDataDirectoryMock(String path) {
        // we mock the file so linux paths are not convert in windows paths and vice-versa
        File mockedPath = EasyMock.createMock(File.class);
        EasyMock.expect(mockedPath.getPath()).andReturn(path).anyTimes();
        // mocked data directory that will use our mocked file
        GeoServerResourceLoader resourceLoader = EasyMock.createMock(GeoServerResourceLoader.class);
        EasyMock.expect(resourceLoader.getBaseDirectory()).andReturn(mockedPath).anyTimes();
        EasyMock.replay(mockedPath, resourceLoader);
        return new GeoServerDataDirectory(resourceLoader);
    }
}
