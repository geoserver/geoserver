/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoserver.platform;

import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author surzhin.konstantin
 */
public class GeoServerResourceLoaderTest {

    /**
     * Test of setApplicationContext method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testSetApplicationContext() {
        System.out.println("setApplicationContext");
        final ApplicationContext applicationContext = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        instance.setApplicationContext(applicationContext);

        assertNull("baseDirectory still points to null.", instance.baseDirectory);
        assertTrue("searchLocations still empty.", instance.searchLocations.isEmpty());
    }

    /**
     * Test of getBaseDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testGetBaseDirectory() {
        System.out.println("getBaseDirectory");
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File result = instance.getBaseDirectory();
        assertEquals("baseDirectory is not null", null, result);
    }

    /**
     * Test of setBaseDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testSetBaseDirectory() {
        System.out.println("setBaseDirectory");
        final File baseDirectory = new File("~");
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        instance.setBaseDirectory(baseDirectory);
        final File result = instance.getBaseDirectory();
        final String msg = "This is test for side Effect of SetBaseDirectory()";
        assertEquals(msg, baseDirectory, result);

        assertFalse(msg, instance.searchLocations.isEmpty());
        assertTrue(msg, instance.searchLocations.size() == 1);
        Object[] arr = instance.searchLocations.toArray();
        assertEquals(msg, baseDirectory, arr[0]);
    }

    /**
     * Test of find method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindEmptyString() throws Exception {
        System.out.println("find empty string");
        final String location = "";
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File result = instance.find(location);
        assertNotNull("Something has returned.", result);
    }

    /**
     * Test of find method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindString() throws Exception {
        System.out.println("find");
        final String location = "~";
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File result = instance.find(location);
        assertNull("Nothing has returned.", result);
    }

    /**
     * Test of findOrCreateDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindOrCreateDirectoryString() throws Exception {
        System.out.println("findOrCreateDirectory");
        final String location = "";
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File result = instance.findOrCreateDirectory(location);
        assertNotNull(result);
    }

    /**
     * Test of findOrCreateDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindOrCreateDirectoryFileString() throws Exception {
        System.out.println("findOrCreateDirectory");
        final File parent = null;
        final String location = "";
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File result = instance.findOrCreateDirectory(parent, location);
        assertNotNull(result);
    }
}
