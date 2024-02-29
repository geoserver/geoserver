/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.resource;

import static org.geoserver.platform.resource.Paths.extension;
import static org.geoserver.platform.resource.Paths.name;
import static org.geoserver.platform.resource.Paths.names;
import static org.geoserver.platform.resource.Paths.parent;
import static org.geoserver.platform.resource.Paths.path;
import static org.geoserver.platform.resource.Paths.sidecar;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import org.junit.Test;

public class PathsTest {

    final String BASE = "";

    final String DIRECTORY = "directory";

    final String FILE = "directory/file.txt";

    final String SIDECAR = "directory/file.prj";

    final String FILE2 = "directory/file2.txt";

    final String SUBFOLDER = "directory/folder";

    final String FILE3 = "directory/folder/file3.txt";

    @Test
    public void pathTest() {
        assertEquals(2, names("a/b").size());
        assertEquals(1, names("a/").size());
        assertEquals(1, names("a").size());
        assertEquals(0, names("").size());

        assertEquals(BASE, path(""));
        assertEquals("directory/file.txt", path("directory", "file.txt"));
        assertEquals("directory/folder/file3.txt", path("directory/folder", "file3.txt"));

        // handling invalid values
        assertNull(path((String) null)); // edge case
        assertEquals("foo", path("foo/"));

        try {
            assertEquals("foo", path(".", "foo"));
            fail(". invalid relative path");
        } catch (IllegalArgumentException expected) {
        }

        try {
            assertEquals("foo", path("foo/bar", ".."));
            fail(".. invalid relative path");
        } catch (IllegalArgumentException expected) {
        }

        // test path elements that are always valid regardless of strictPath
        for (String name : new String[] {"foo", "foo.txt", "directory/bar"}) {
            assertEquals(name, Paths.path(name));
        }
        // test path elements that are always invalid regardless of strictPath
        for (String name : new String[] {".", "..", "foo\\"}) {
            try {
                assertEquals(name, Paths.path(name));
                fail("invalid: " + name);
            } catch (IllegalArgumentException expected) {
                // ignore
            }
        }
        // test path elements that are invalid if and only if strictPath is true
        for (char c : "*:,'&?\"<>|".toCharArray()) {
            for (String prefix : new String[] {"foo", ""}) {
                for (String suffix : new String[] {"bar", ""}) {
                    String name = prefix + c + suffix;
                    assertEquals(name, Paths.path(name));
                }
            }
        }
    }

    @Test
    public void validTest() {
        // test path elements that are always valid regardless of strictPath
        for (String name : new String[] {"foo", "foo.txt", "directory/bar"}) {
            assertEquals(name, Paths.valid(name));
        }
        // test path elements that are always invalid regardless of strictPath
        for (String name : new String[] {".", "..", "foo\\"}) {
            try {
                assertEquals(name, Paths.valid(name));
                fail("invalid: " + name);
            } catch (IllegalArgumentException expected) {
                // ignore
            }
        }
        // test path elements that are invalid if and only if strictPath is true
        for (char c : "*:,'&?\"<>|".toCharArray()) {
            for (String prefix : new String[] {"foo", ""}) {
                for (String suffix : new String[] {"bar", ""}) {
                    String name = prefix + c + suffix;
                    assertEquals(name, Paths.valid(name));
                }
            }
        }
    }

    @Test
    public void isValidTest() {
        // test paths with . and .. are not valid
        for (String name :
                new String[] {"directory/./bar", "directory/../bar", "../bar", "./bar"}) {
            assertFalse(name, Paths.isValid(name));
        }
    }

    @Test
    public void parentTest() {
        assertEquals(DIRECTORY, parent(FILE));
        assertEquals(BASE, parent(DIRECTORY));
        assertNull(parent(BASE));

        // handling invalid values
        assertNull(null, parent(null));
        assertEquals("foo", parent("foo/"));
    }

    @Test
    public void naming() throws IOException {
        assertEquals("file.txt", name("directory/file.txt"));
        assertEquals("txt", extension("directory/file.txt"));

        assertEquals("directory/file.txt", sidecar("directory/file", "txt"));
        assertEquals("directory/file.prj", sidecar("directory/file.txt", "prj"));
    }

    @Test
    public void convert1() throws IOException {
        // relative paths
        File folder = new File("folder");
        File file1 = new File("file1");
        File file2 = new File(folder, "file2");

        assertEquals("folder", Paths.convert(folder.getPath()));
        assertEquals("folder/file2", Paths.convert(file2.getPath()));
        assertEquals("file1", Paths.convert(file1.getPath()));

        // absolute paths
        File home = new File(System.getProperty("user.home")).getCanonicalFile();
        assertTrue(FilePaths.isAbsolute(Paths.convert(home.getPath())));
    }

    @Test
    public void convert2() {
        File home = new File(System.getProperty("user.home"));
        File directory = new File(home, "directory");
        File folder = new File(directory, "folder");
        File file1 = new File(directory, "file1");
        File file2 = new File(folder, "file2");
        File relative = new File(new File(".."), "file1");

        assertEquals("folder", Paths.convert(directory, folder));
        assertEquals("folder/file2", Paths.convert(directory, file2));
        assertEquals("file1", Paths.convert(directory, file1));

        String relativePath = relative.getPath();
        assertEquals("file1", Paths.convert(directory, folder, relativePath));
    }

    @Test
    public void validationConsistency() {
        checkValid(false, null);
        checkValid(true, "");
        checkValid(false, "relatively/../uncomfortable");
        checkValid(false, "current/./events");
        checkValid(false, ".");
        checkValid(false, "..");
        checkValid(true, "directory/");
        checkValid(true, "directory/file.txt");
        checkValid(true, "image.png");

        // valid but inadvisable
        checkValid(true, "grep.*");
    }

    private void checkValid(boolean isValid, String path) {
        assertEquals("validate: " + path, isValid, Paths.isValid(path));
        if (isValid) {
            assertEquals(path, Paths.valid(path));
        } else {
            try {
                assertNotEquals(path, Paths.valid(null));
                fail("Expected '" + path + "' the fail validation with an exception");
            } catch (NullPointerException | IllegalStateException expected) {
            }
        }
    }
}
