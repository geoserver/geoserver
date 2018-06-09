/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static org.junit.Assert.assertTrue;

import com.google.common.io.Files;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IOUtilsTest {

    @Rule public TemporaryFolder temp = new TemporaryFolder(new File("target"));

    @Test
    public void testZipUnzip() throws IOException {
        Path p1 = temp.newFolder("d1").toPath();
        p1.resolve("foo/bar").toFile().mkdirs();
        Files.touch(p1.resolve("foo/bar/bar.txt").toFile());

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(bout);

        IOUtils.zipDirectory(p1.toFile(), zout, null);

        Path p2 = temp.newFolder("d2").toPath();
        p2.toFile().mkdirs();

        ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
        IOUtils.decompress(bin, p2.toFile());

        assertTrue(p2.resolve("foo/bar/bar.txt").toFile().exists());
    }
}
