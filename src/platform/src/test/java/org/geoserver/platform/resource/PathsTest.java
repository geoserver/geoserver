package org.geoserver.platform.resource;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import static org.geoserver.platform.resource.Paths.*;

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

        if (Paths.STRICT_PATH == true) {
            try {
                assertEquals("foo?", path("foo?"));
                fail("? invalid character");
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    @Test
    public void validTest() {
        List<String> valid = Arrays.asList(new String[] { "foo","foo.txt","directory/bar" });
        for (String name : valid) {
            assertEquals(name, Paths.valid(name));
        }
        
        List<String> invalid = Arrays.asList(new String[] { ".", "..", "foo\\" });
        for (String name : invalid) {
            try {
                assertEquals(name, Paths.valid(name));
                fail("invalid:" + name);
            } catch (IllegalArgumentException expected) {
            }
        }
        
        for (String name : new String[]{"foo:*,\'&?\"<>|bar"}) {
            if (Paths.STRICT_PATH == true) {
                try {
                    assertEquals(name, Paths.valid(name));
                    fail("invalid:" + name);
                } catch (IllegalArgumentException expected) {
                }
            } else {
                assertEquals(name, Paths.valid(name));
            }
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
    public void naming() {
        assertEquals("file.txt", name("directory/file.txt"));
        assertEquals("txt", extension("directory/file.txt"));

        assertEquals("directory/file.txt", sidecar("directory/file", "txt"));
        assertEquals("directory/file.prj", sidecar("directory/file.txt", "prj"));
    }

    @Test
    public void convert1() {
        File folder = new File("folder");
        File file1 = new File("file1");
        File file2 = new File(folder, "file2");

        assertEquals("folder", Paths.convert(folder.getPath()));
        assertEquals("folder/file2", Paths.convert(file2.getPath()));
        assertEquals("file1", Paths.convert(file1.getPath()));
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
}
