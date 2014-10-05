/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoserver.platform;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author surzhin.konstantin
 */
public class GeoServerResourceLoaderIOTest {

    /**
     * Test of createDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCreateDirectoryString() throws Exception {
        System.out.println("createDirectory");
        final String location = "";
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;
        try {
            final File result = instance.createDirectory(location);
            assertEquals(expResult, result);
        } catch (IOException ioe) {
            fail("The test case to catch IOE in the createDirectory: " + ioe.getMessage());
        }
    }

    /**
     * Test of createDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCreateDirectoryFileString() throws Exception {
        System.out.println("createDirectory");
        final File parent = null;
        final String location = "";
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.createDirectory(parent, location);
            assertEquals(expResult, result);
        } catch (IOException ioe) {
            fail("The test case to catch IOE in the createDirectory: " + ioe.getMessage());
        }
    }
}
