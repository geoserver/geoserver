/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Test;

public class FilePathsTest {

    @Test
    public void isAbsolutePath() {
        assertFalse("data directory relative", FilePaths.isAbsolute("data/tasmania/roads.shp"));
        assertTrue(
                "linux absolute", FilePaths.isAbsolute("/srv/gis/cadaster/district.geopkg", false));
        assertTrue(
                "windows drive absolute",
                FilePaths.isAbsolute("D:/gis/cadaster/district.geopkg", true));
        assertFalse("windows drive relative", FilePaths.isAbsolute("D:fail.shp", true));
    }

    @Test
    public void naming() throws IOException {
        File home = new File(System.getProperty("user.home")).getCanonicalFile();
        String absolutePath = Paths.convert(home.getPath());

        assertTrue("home", FilePaths.isAbsolute(absolutePath));
        String root = FilePaths.names(absolutePath).get(0);
        assertTrue("system root '" + root + "'", FilePaths.isAbsolute(root));
    }

    @Test
    public void roundTripFileTests() throws IOException {
        File home = new File(System.getProperty("user.home")).getCanonicalFile();
        File directory = new File(home, "directory");
        File file = new File(directory, "file");

        // absolute paths
        absolutePathCheck(home);
        absolutePathCheck(directory);
        absolutePathCheck(file);

        // relative paths
        File base = new File(home, "data");
        relativePathCheck(base, new File("folder"));
        relativePathCheck(base, new File(new File("folder"), "file"));
    }

    public void absolutePathCheck(File file) {
        String filePath = file.getPath();
        String path1 = Paths.convert(filePath);
        assertTrue("absolute: " + path1, FilePaths.isAbsolute(path1));

        List<String> names = FilePaths.names(path1);
        assertTrue("absolute: " + names.get(0), FilePaths.isAbsolute(names.get(0)));

        String path2 = Paths.toPath(names);
        assertTrue("absolute:" + path2, FilePaths.isAbsolute(path2));

        File file2 = FilePaths.toFile(null, path2);
        assertEquals(file2.getName(), file, file2);
    }

    public void relativePathCheck(File base, File file) {
        String path1 = Paths.convert(base, file);
        assertFalse("absolute: " + path1, FilePaths.isAbsolute(path1));

        List<String> names = Paths.names(path1);
        assertFalse("absolute: " + names.get(0), FilePaths.isAbsolute(names.get(0)));

        String path2 = Paths.toPath(names);
        assertFalse("absolute:" + path2, FilePaths.isAbsolute(path2));

        File file2 = FilePaths.toFile(base, path2);
        assertTrue("absolute:" + file2.getPath(), file2.isAbsolute());
        assertEquals(file2.getName(), new File(base, file.getPath()), file2);
    }
}
