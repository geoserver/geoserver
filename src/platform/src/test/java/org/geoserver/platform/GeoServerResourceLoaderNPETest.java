/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoserver.platform;

import java.io.File;
import javax.servlet.ServletContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author surzhin.konstantin
 */
public class GeoServerResourceLoaderNPETest {

    /**
     * Test of addSearchLocation method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testAddSearchLocation() {
        System.out.println("addSearchLocation");
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();

        try {
            instance.addSearchLocation(null);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    /**
     * Test of setSearchLocations method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testSetSearchLocations() {
        System.out.println("setSearchLocations");
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();

        try {
            instance.setSearchLocations(null);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the setSearchLocations.");
        }
    }

    @Test
    public final void testSetBaseDirectoryNull() {
        System.out.println("setBaseDirectory to null");
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();

        try {
            instance.setBaseDirectory(null);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the setBaseDirectory.");
        }
    }

    /**
     * Test of find method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindStringNull() throws Exception {
        System.out.println("find");
        final String location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;

        try {
            File result = instance.find(location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the find(String).");
        }
    }

    /**
     * Test of find method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindFileString() throws Exception {
        System.out.println("find");
        final File parent = null;
        final String location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;

        try {
            final File result = instance.find(parent, location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the find(File,String).");
        }
    }

    /**
     * Test of find method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindStringArr() throws Exception {
        System.out.println("find");
        final String[] location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.find(location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    /**
     * Test of find method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindFileStringArr() throws Exception {
        System.out.println("find");
        final File parent = null;
        final String[] location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.find(parent, location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE (File - StringArr).");
        }
    }

    /**
     * Test of concat method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testConcat() {
        System.out.println("concat");
        final String[] location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final String expResult = "";

        try {
            final String result = instance.concat(location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    /**
     * Test of findOrCreateDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindOrCreateDirectoryStringArr() throws Exception {
        System.out.println("findOrCreateDirectory");
        final String[] location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.findOrCreateDirectory(location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    /**
     * Test of findOrCreateDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindOrCreateDirectoryNullFileNullStringArr() throws Exception {
        System.out.println("findOrCreateDirectory");
        final File parent = null;
        final String[] location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.findOrCreateDirectory(parent, location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    @Test
    public final void testFindOrCreateDirectorNullString() throws Exception {
        System.out.println("findOrCreateDirectory");
        final String location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            File result = instance.findOrCreateDirectory(location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the findOrCreateDirectory");
        }
    }

    @Test
    public final void testFindOrCreateDirectoryNullFileNullString() throws Exception {
        System.out.println("findOrCreateDirectory");
        final File parent = null;
        final String location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.findOrCreateDirectory(parent, location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the findOrCreateDirectory");
        }
    }

    /**
     * Test of createDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCreateDirectoryNullStringArr() throws Exception {
        System.out.println("createDirectory");
        final String[] location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.createDirectory(location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    /**
     * Test of createDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCreateDirectoryNullFileNullStringArr() throws Exception {
        System.out.println("createDirectory");
        final File parent = null;
        final String[] location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.createDirectory(parent, location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    /**
     * Test of createDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCreateDirectoryNullString() throws Exception {
        System.out.println("createDirectory");
        final String location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.createDirectory(location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the createDirectory");
        }
    }

    /**
     * Test of createDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCreateDirectoryNullFileNullString() throws Exception {
        System.out.println("createDirectory");
        final File parent = null;
        final String location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.createDirectory(parent, location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the createDirectory");
        }
    }

    /**
     * Test of createFile method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCreateFileNullStringArr() throws Exception {
        System.out.println("createFile");
        final String[] location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.createFile(location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    /**
     * Test of createFile method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCreateFileNullString() throws Exception {
        System.out.println("createFile");
        final String location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.createFile(location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the setSearchLocations.");
        }
    }

    /**
     * Test of createFile method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCreateFileNullFileNullStringArr() throws Exception {
        System.out.println("createFile");
        final File parent = null;
        final String[] location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.createFile(parent, location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    /**
     * Test of createFile method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCreateFileNullFileNullString() throws Exception {
        System.out.println("createFile");
        final File parent = null;
        final String location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        final File expResult = null;

        try {
            final File result = instance.createFile(parent, location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    /**
     * Test of copyFromClassPath method, of class GeoServerResourceLoader.
     */
    @Test
    public void testCopyFromClassPathStringString() throws Exception {
        System.out.println("copyFromClassPath");
        final String resource = null;
        final String to = null;
        GeoServerResourceLoader instance = new GeoServerResourceLoader();

        try {
            instance.copyFromClassPath(resource, to);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE (String - String).");
        }
    }

    /**
     * Test of copyFromClassPath method, of class GeoServerResourceLoader.
     *
     * @throws java.lang.Exception
     */
    @Test
    public final void testCopyFromClassPathStringFile() throws Exception {
        System.out.println("copyFromClassPath");
        final String resource = null;
        final File target = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();

        try {
            instance.copyFromClassPath(resource, target);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE (String - File).");
        }
    }

    /**
     * Test of copyFromClassPath method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testCopyFromClassPathNull3args() throws Exception {
        System.out.println("copyFromClassPath");
        final String resource = null;
        final File target = null;
        final Class scope = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();

        try {
            instance.copyFromClassPath(resource, target, scope);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE (3args).");
        }
    }

    /**
     * Test of lookupGeoServerDataDirectory method, of class
     * GeoServerResourceLoader.
     */
    @Test
    public void testLookupGeoServerDataDirectory() {
        System.out.println("lookupGeoServerDataDirectory");
        final ServletContext servContext = null;
        final String expResult = "";

        try {
            final String result = GeoServerResourceLoader.lookupGeoServerDataDirectory(servContext);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

}
