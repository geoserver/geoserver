/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import java.io.File;

import javax.servlet.ServletContext;

import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for {@link GeoServerResourceLoader}.
 */
public class GeoServerResourceLoaderTest {

    @Rule
    public final ExpectedException expected = ExpectedException.none();

    /**
     * Test {@link GeoServerResourceLoader#requireFile(String, String)} for a single file that exists.
     */
    @Test
    public void testRequireSingleExistingFile() {
        GeoServerResourceLoader.requireFile("pom.xml", "Test fixture");
    }

    /**
     * Test {@link GeoServerResourceLoader#requireFile(String, String)} for two files that exist.
     */
    @Test
    public void testRequireTwoExistingFiles() {
        GeoServerResourceLoader.requireFile("pom.xml" + File.pathSeparator + "src", "Test fixture");
    }

    /**
     * Test {@link GeoServerResourceLoader#requireFile(String, String)} for a single file that does not exist.
     */
    @Test
    public void testRequireSingleMissingFile() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage(
                "Missing required file: does-not-exist From: Test fixture: does-not-exist");
        GeoServerResourceLoader.requireFile("does-not-exist", "Test fixture");
    }

    /**
     * Test {@link GeoServerResourceLoader#requireFile(String, String)} for two files where one does not exist.
     */
    @Test
    public void testRequireSingleMissingFileOfTwo() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("Missing required file: does-not-exist From: Test fixture: pom.xml"
                + File.pathSeparator + "does-not-exist");
        GeoServerResourceLoader.requireFile("pom.xml" + File.pathSeparator + "does-not-exist",
                "Test fixture");
    }

    /**
     * Test {@link GeoServerResourceLoader#lookupGeoServerDataDirectory(ServletContext)} with a single required file that exists specified in the Java
     * environment.
     */
    @Test
    public void testLookupRequireExistingFileJava() {
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(context.getInitParameter("GEOSERVER_REQUIRE_FILE")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_DIR")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_ROOT")).andReturn(null);
        EasyMock.expect(context.getRealPath("/data")).andReturn("data");
        EasyMock.replay(context);
        System.setProperty("GEOSERVER_REQUIRE_FILE", "pom.xml");
        try {
            Assert.assertEquals("data",
                    GeoServerResourceLoader.lookupGeoServerDataDirectory(context));
        } finally {
            System.clearProperty("GEOSERVER_REQUIRE_FILE");
        }
    }

    /**
     * Test {@link GeoServerResourceLoader#lookupGeoServerDataDirectory(ServletContext)} with a single required file that does not exist specified in
     * the Java environment.
     */
    @Test
    public void testLookupRequireMissingFileJava() {
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(context.getInitParameter("GEOSERVER_REQUIRE_FILE")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_DIR")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_ROOT")).andReturn(null);
        EasyMock.expect(context.getRealPath("/data")).andReturn("data");
        EasyMock.replay(context);
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("Missing required file: does-not-exist "
                + "From: Java environment variable GEOSERVER_REQUIRE_FILE: does-not-exist");
        System.setProperty("GEOSERVER_REQUIRE_FILE", "does-not-exist");
        try {
            GeoServerResourceLoader.lookupGeoServerDataDirectory(context);
        } finally {
            System.clearProperty("GEOSERVER_REQUIRE_FILE");
        }
    }

    /**
     * Test {@link GeoServerResourceLoader#lookupGeoServerDataDirectory(ServletContext)} with a single required file that does not exist specified in
     * the servlet context.
     */
    @Test
    public void testLookupRequireMissingFileServlet() {
        ServletContext context = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(context.getInitParameter("GEOSERVER_REQUIRE_FILE"))
                .andReturn("does-not-exist");
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_DIR")).andReturn(null);
        EasyMock.expect(context.getInitParameter("GEOSERVER_DATA_ROOT")).andReturn(null);
        EasyMock.expect(context.getRealPath("/data")).andReturn("data");
        EasyMock.replay(context);
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("Missing required file: does-not-exist "
                + "From: Servlet context parameter GEOSERVER_REQUIRE_FILE: does-not-exist");
        GeoServerResourceLoader.lookupGeoServerDataDirectory(context);
    }

}
