/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.vsi;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;
import org.gdal.gdal.gdal;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for VSIProperties class
 *
 * @author Matthew Northcott <matthewnorthcott@catalyst.net.nz>
 */
public class VSIPropertiesTest {

    private VSITestHelper helper = new VSITestHelper();

    @Before
    public void setUp() {
        helper.setVSIPropertiesToResource(helper.PROPERTIES_VALID);
    }

    @After
    public void tearDown() {
        System.clearProperty(VSIProperties.LOCATION_PROPERTY);
    }

    @Test(expected = IOException.class)
    public void testParsePropertiesUnset() throws IOException {
        System.clearProperty(VSIProperties.LOCATION_PROPERTY);

        VSIProperties.getProperties();
    }

    @Test(expected = FileNotFoundException.class)
    public void testParsePropertiesDoesNotExist() throws IOException {
        System.setProperty(VSIProperties.LOCATION_PROPERTY, helper.PROPERTIES_NO_EXIST);

        VSIProperties.getProperties();
    }

    @Test
    public void testParsePropertiesEmpty() throws IOException {
        helper.setVSIPropertiesToResource(helper.PROPERTIES_EMPTY);

        assertTrue(VSIProperties.getProperties().isEmpty());
    }

    @Test
    public void testParsePropertiesValid() throws IOException {
        helper.setVSIPropertiesToResource(helper.PROPERTIES_VALID);

        final Properties properties = VSIProperties.getProperties();

        assertEquals(7, properties.size());
        assertEquals(properties.getProperty("OS_PROJECT_DOMAIN_NAME"), "default");
        assertEquals(properties.getProperty("OS_USER_DOMAIN_NAME"), "Default");
        assertEquals(properties.getProperty("OS_IDENTITY_API_VERSION"), "3");
        assertEquals(properties.getProperty("OS_AUTH_URL"), helper.AUTHENTICATION_URL);
        assertEquals(properties.getProperty("OS_PROJECT_NAME"), helper.PROJECT_NAME);
        assertEquals(properties.getProperty("OS_USERNAME"), helper.USERNAME);
        assertEquals(properties.getProperty("OS_PASSWORD"), helper.PASSWORD);
    }

    @Test
    public void testAuthenticate() throws IOException, URISyntaxException {
        helper.setVSIPropertiesToResource(helper.PROPERTIES_VALID);

        VSIProperties.sync();
        assertEquals(gdal.GetConfigOption("OS_PROJECT_DOMAIN_NAME"), "default");
        assertEquals(gdal.GetConfigOption("OS_USER_DOMAIN_NAME"), "Default");
        assertEquals(gdal.GetConfigOption("OS_IDENTITY_API_VERSION"), "3");
        assertEquals(gdal.GetConfigOption("OS_AUTH_URL"), helper.AUTHENTICATION_URL);
        assertEquals(gdal.GetConfigOption("OS_PROJECT_NAME"), helper.PROJECT_NAME);
        assertEquals(gdal.GetConfigOption("OS_USERNAME"), helper.USERNAME);
        assertEquals(gdal.GetConfigOption("OS_PASSWORD"), helper.PASSWORD);
    }
}
