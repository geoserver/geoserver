/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IOUtilsTest {

    @Rule public TemporaryFolder temp = new TemporaryFolder(new File("target"));

    @Test
    public void testInflateBadEntryName() throws IOException {
        File destDir = temp.newFolder("d1").toPath().toFile();
        destDir.mkdirs();
        Resource directory = new GeoServerResourceLoader(destDir).get("");
        try (InputStream input = getClass().getResourceAsStream("/bad-zip-file.zip")) {
            File file = new File(destDir, "bad-zip-file.zip");
            IOUtils.copyStream(input, new FileOutputStream(file), false, true);
            IOUtils.inflate(new ZipFile(file), directory, null, null, null, null, false, false);
            fail("Expected decompression to fail");
        } catch (IOException e) {
            assertTrue(e.getMessage().startsWith("Entry is outside of the target directory"));
        }
    }
}
