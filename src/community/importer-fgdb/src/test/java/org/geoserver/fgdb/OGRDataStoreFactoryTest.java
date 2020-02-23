/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.fgdb;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.ogr.OGRDataStoreFactory;
import org.geotools.data.ogr.jni.JniOGRDataStoreFactory;
import org.geotools.util.KVP;
import org.geotools.util.URLs;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.type.Name;

public class OGRDataStoreFactoryTest {
    /** If OGR environment is not available factory will be null and the test skipped. */
    static boolean skip = false;

    /** Factory obtained during setup. */
    private static OGRDataStoreFactory factory = null;

    private static Logger LOGGING;

    private static Level loggingLevel;

    private URL resourceURL;

    private File file;

    private File gdbFile;

    private URL gdbURL;

    private File locationsFile;

    @BeforeClass
    public static void beforeClass() {
        // check Environment is available
        factory = new JniOGRDataStoreFactory();
        skip = !factory.isAvailable();
        if (skip) {
            System.out.println("GDBDataStoreFactoryTest skipped - Check OGR Environment");
            System.out.println("System Environment Variables:");
            System.out.println("GDAL_DATA = " + System.getenv("GDAL_DATA"));
            System.out.println("DYLD_LIBRARY_PATH = " + System.getenv("DYLD_LIBRARY_PATH"));
            System.out.println("LD_LIBRARY_PATH = " + System.getenv("LD_LIBRARY_PATH"));
            System.out.println("GDAL_DRIVER_PATH = " + System.getenv("GDAL_DRIVER_PATH"));
            System.out.println("CPL_LOG = " + System.getenv("CPL_LOG"));
            System.out.println("CPL_LOG_ERRORS = " + System.getenv("CPL_LOG_ERRORS"));
            System.out.println("System Environment:");

            System.out.println("Java System Properties:");
            System.out.println("java.library.path = " + System.getProperty("java.library.path"));
        }
        LOGGING = org.geotools.util.logging.Logging.getLogger("org.geotools.data.ogr");
        loggingLevel = LOGGING.getLevel();
        LOGGING.setLevel(Level.FINEST);
    }

    @AfterClass
    public static void afterClass() {
        LOGGING.setLevel(loggingLevel);
    }

    @Before
    public void before() throws Exception {
        if (skip) return;

        File archive = data("locations.zip");
        File unpack = File.createTempFile("importer", "data", new File("target"));
        unpack.delete();
        unpack.mkdirs();
        unpack(archive, unpack);
        resourceURL = URLs.fileToUrl(unpack);
        System.out.println("locations.gdb unpacked into: " + resourceURL);

        assert resourceURL != null : "Could not find locations.gdb resource";
        File resource = URLs.urlToFile(resourceURL);

        File gdbDirectory = new File(resource, "locations.gdb");

        gdbFile = new File(gdbDirectory, "gdb");
        gdbURL = URLs.fileToUrl(gdbFile);

        System.out.println("Unpacked URL: " + resourceURL);
        System.out.println("Unpacked File: " + resource);
        System.out.println("Locations Dir: " + gdbDirectory);
        System.out.println("Locations GDB: " + gdbFile);
        System.out.println("Locations URL: " + gdbURL);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDataStoreURL() throws MalformedURLException, IOException {
        if (skip) return;

        Map<String, Serializable> params = (Map) new KVP(OGRDataStoreFactory.OGR_NAME.key, gdbURL);
        DataStore dataStore = factory.createDataStore(params);
        assertNotNull("Failure creating data store", dataStore);

        List<Name> names = dataStore.getNames();
        assertEquals(1, names.size());
    }

    private static File data(String path) throws IOException {
        URL url = OGRDataStoreFactory.class.getResource("test-data/" + path);
        if (url == null) {
            throw new FileNotFoundException("Could not find locations.zip");
        }
        File file = new File(URLDecoder.decode(url.getPath(), "UTF-8"));
        if (!file.exists()) {
            throw new FileNotFoundException("Can not locate test-data for \"" + path + '"');
        }
        return file;
    }

    //
    // Utility Methods
    //
    private static void unpackFile(ZipInputStream in, File outdir, String name) throws IOException {
        File file = new File(outdir, name);
        FileOutputStream out = new FileOutputStream(file);
        try {
            IOUtils.copy(in, out);
        } finally {
            out.close();
        }
    }

    private static boolean mkdirs(File outdir, String path) {
        if (path == null) return false;
        File directory = new File(outdir, path);
        if (!directory.exists()) {
            return directory.mkdirs();
        }
        return false;
    }

    private static String directoryName(String name) {
        int s = name.lastIndexOf(File.separatorChar);
        return s == -1 ? null : name.substring(0, s);
    }

    /**
     * * Unpack archive to directory (maintaining directory structure).
     *
     */
    public static void unpack(File archive, File directory) throws IOException {
        // see http://stackoverflow.com/questions/10633595/java-zip-how-to-unzip-folder
        ZipInputStream zip = new ZipInputStream(new FileInputStream(archive));
        try {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    mkdirs(directory, name);
                    continue;
                }
                /*
                 * this part is necessary because file entry can come before directory entry where is file located i.e.: /foo/foo.txt /foo/
                 */
                String dir = directoryName(name);
                if (dir != null) {
                    mkdirs(directory, dir);
                }
                unpackFile(zip, directory, name);
            }
            zip.close();
        } finally {
            zip.close();
        }
    }
}
