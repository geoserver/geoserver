/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.geoserver.platform;

import java.io.File;
import javax.servlet.ServletContext;
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

    public GeoServerResourceLoaderTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setApplicationContext method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testSetApplicationContext() {
        System.out.println("setApplicationContext");
        ApplicationContext applicationContext = null;
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        instance.setApplicationContext(applicationContext);

        assertNull("baseDirectory still points to null.", instance.baseDirectory);
        assertTrue("searchLocations still empty.", instance.searchLocations.isEmpty());
    }

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

    /**
     * Test of getBaseDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testGetBaseDirectory() {
        System.out.println("getBaseDirectory");
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File result = instance.getBaseDirectory();
        assertEquals("baseDirectory is not null", null, result);
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
     * Test of setBaseDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testSetBaseDirectory() {
        System.out.println("setBaseDirectory");
        final File baseDirectory = new File("~");
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();
        instance.setBaseDirectory(baseDirectory);
        final File result = instance.getBaseDirectory();
        String msg = "This is test for side Effect of SetBaseDirectory()";
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
     * Test of find method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFindFileString() throws Exception {
        System.out.println("find");
        File parent = null;
        String location = null;
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;

        try {
            File result = instance.find(parent, location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the find(File,String).");
        }
    }

    /**
     * Test of find method, of class GeoServerResourceLoader.
     */
    @Test
    public final void testFind_StringArr() throws Exception {
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
    public void testFindFileStringArr() throws Exception {
        System.out.println("find");
        File parent = null;
        String[] location = null;
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;

        try {
            File result = instance.find(parent, location);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE (File - StringArr).");
        }

        //assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
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
    public final void testFindOrCreateDirectory_File_StringArr() throws Exception {
        System.out.println("findOrCreateDirectory");
        final File parent = null;
        final String[] location = null;
        final GeoServerResourceLoader instance = new GeoServerResourceLoader();

        try {
            final File expResult = null;
            final File result = instance.findOrCreateDirectory(parent, location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    /**
     * Test of findOrCreateDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public void testFindOrCreateDirectoryString() throws Exception {
        System.out.println("findOrCreateDirectory");
        String location = "";
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;
        File result = instance.findOrCreateDirectory(location);
        assertEquals(expResult, result);
    }

    @Test
    public void testFindOrCreateDirectorNullString() throws Exception {
        System.out.println("findOrCreateDirectory");
        String location = null;
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;

        try {
            File result = instance.findOrCreateDirectory(location);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the findOrCreateDirectory");
        }
    }

    /**
     * Test of findOrCreateDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public void testFindOrCreateDirectory_File_String() throws Exception {
        System.out.println("findOrCreateDirectory");
        File parent = null;
        String location = "";
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;
        File result = instance.findOrCreateDirectory(parent, location);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of createDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public void testCreateDirectory_StringArr() throws Exception {
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
    public void testCreateDirectory_File_StringArr() throws Exception {
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
    public void testCreateDirectory_String() throws Exception {
        System.out.println("createDirectory");
        String location = "";
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;
        File result = instance.createDirectory(location);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of createDirectory method, of class GeoServerResourceLoader.
     */
    @Test
    public void testCreateDirectory_File_String() throws Exception {
        System.out.println("createDirectory");
        File parent = null;
        String location = "";
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;
        File result = instance.createDirectory(parent, location);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of createFile method, of class GeoServerResourceLoader.
     */
    @Test
    public void testCreateFile_StringArr() throws Exception {
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
    public final void testCreateFileString() throws Exception {
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
    public void testCreateFile_File_StringArr() throws Exception {
        System.out.println("createFile");
        File parent = null;
        String[] location = null;
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;

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
    public void testCreateFile_File_String() throws Exception {
        System.out.println("createFile");
        File parent = null;
        String location = "";
        GeoServerResourceLoader instance = new GeoServerResourceLoader();
        File expResult = null;
        File result = instance.createFile(parent, location);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
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
    public final void testCopyFromClassPath3args() throws Exception {
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
        ServletContext servContext = null;
        String expResult = "";

        try {
            final String result = GeoServerResourceLoader.lookupGeoServerDataDirectory(servContext);
            assertEquals(expResult, result);
        } catch (NullPointerException npe) {
            fail("The test case to catch NPE in the addSearchLocation");
        }
    }

    }
