/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.util;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipFile;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.ZipTestUtil;
import org.geotools.data.ows.URLChecker;
import org.geotools.data.ows.URLCheckers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IOUtilsTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder(new File("target"));

    /** URLCheck used to restrict content to schemas.opengis.net. */
    private static URLChecker opengisChecker = new URLChecker() {
        @Override
        public String getName() {
            return "schemas.opengis.net";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public boolean confirm(String s) {
            return s.startsWith("https://schemas.opengis.net/");
        }
    };

    private static URLChecker tmpChecker = new URLChecker() {
        String tmpDir = new File(System.getProperty("java.io.tmpdir"))
                .getAbsoluteFile()
                .toURI()
                .toString();

        @Override
        public String getName() {
            return "java.io.tmpdir";
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public boolean confirm(String s) {
            return s.startsWith(tmpDir);
        }
    };

    @BeforeClass
    public static void setupURLCheckers() throws Exception {
        URLCheckers.register(opengisChecker);
        URLCheckers.register(tmpChecker);
    }

    @AfterClass
    public static void teardownURLCheckers() throws Exception {
        URLCheckers.deregister(opengisChecker);
        URLCheckers.deregister(tmpChecker);
    }

    @Test
    public void testInflateBadEntryName() throws IOException {
        File destDir = temp.newFolder("d1").toPath().toFile();
        destDir.mkdirs();
        Resource directory = new GeoServerResourceLoader(destDir).get("");
        File file = ZipTestUtil.initZipSlipFile(temp.newFile("d1.zip"));
        try {
            IOUtils.inflate(new ZipFile(file), directory, null, null, null, null, false, false);
            fail("Expected decompression to fail");
        } catch (IOException e) {
            assertThat(e.getMessage(), startsWith("Entry is outside of the target directory"));
        }
    }

    @Test
    public void testURLToFile() throws Exception {
        File file = File.createTempFile("sample", "txt");
        URL fileURL = file.toURI().toURL();
        assertEquals(file, IOUtils.URLToFile(fileURL));

        File home = new File(System.getProperty("user.home"));
        URL homeURL = home.toURI().toURL();
        assertNull(IOUtils.URLToFile(homeURL));
    }

    @Test
    public void testUpload() throws IOException {
        File destDir = temp.newFolder("upload").toPath().toFile();
        destDir.mkdirs();
        Resource newFile = new GeoServerResourceLoader(destDir).get("vehicles.xml");

        URL uploadURL = new URL("https://schemas.opengis.net/movingfeatures/1.0/examples/vehicles.xml");
        IOUtils.upload(uploadURL, newFile);
        assertSame("uploaded", newFile.getType(), Resource.Type.RESOURCE);

        URL osgeoURL = new URL("https://geoserver.org/img/osgeo-logo.png");
        Resource logoFile = new GeoServerResourceLoader(destDir).get("osgeo-logo.png");
        try {
            IOUtils.upload(osgeoURL, logoFile);
            fail("geoserver.org blocked");
        } catch (Exception failed) {
        }
        assertSame("blocked", logoFile.getType(), Resource.Type.UNDEFINED);
    }
}
