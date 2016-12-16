/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class ConfigStoreTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ResourceStore dataDir;

    private ConfigStore store;

    @Before
    public void before() {
        File root = tempFolder.getRoot();
        dataDir = new FileSystemResourceStore(root);
        store = new ConfigStore(dataDir);
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void saveNull() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("null RepositoryInfo");
        store.save(null);
    }

    @Test
    public void saveNoId() throws Exception {
        RepositoryInfo info = new RepositoryInfo();
        info.setLocation(URI.create("file:/home/test/repo"));
        info.setId(null);
        assertNull(info.getId());
        store.save(info);
        assertNotNull(info.getId());
    }

    @Test
    public void saveDeprecatedFormat() throws Exception {
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966988";
        RepositoryInfo info = new RepositoryInfo(dummyId);
        info.setName("repo");
        info.setParentDirectory("/home/test");
        store.save(info);

        String path = ConfigStore.path(info.getId());
        Resource resource = dataDir.get(path);
        assertTrue(resource.file().exists());
        // Files.copy(resource.file(), System.err);
        String expected = "<RepositoryInfo>"//
                + "<id>" + dummyId + "</id>"//
                + "<location>" + info.getLocation().toString() + "</location>"//
                + "</RepositoryInfo>";

        XMLAssert.assertXMLEqual(new StringReader(expected),
                new InputStreamReader(resource.in(), Charsets.UTF_8));
    }

    @Test
    public void save() throws Exception {
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966988";
        RepositoryInfo info = new RepositoryInfo(dummyId);
        info.setLocation(URI.create("file:/home/test/repo"));
        store.save(info);

        String path = ConfigStore.path(info.getId());
        Resource resource = dataDir.get(path);
        assertTrue(resource.file().exists());
        // Files.copy(resource.file(), System.err);
        String expected = "<RepositoryInfo>"//
                + "<id>" + dummyId + "</id>"//
                + "<location>file:/home/test/repo</location>"//
                + "</RepositoryInfo>";

        XMLAssert.assertXMLEqual(new StringReader(expected),
                new InputStreamReader(resource.in(), Charsets.UTF_8));
    }

    @Test
    public void checkIdFormatOnSave() {
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966988";
        RepositoryInfo info = new RepositoryInfo(dummyId + "a");
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Id doesn't match UUID format");
        store.save(info);
    }

    @Test
    public void loadNull() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("null id");
        store.get(null);
    }

    @Test
    public void loadNonExistent() throws Exception {
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966989";
        try {
            store.get(dummyId);
            fail("Expected FileNotFoundException");
        } catch (FileNotFoundException e) {
            assertTrue(e.getMessage().startsWith("File not found: "));
        }

        String path = ConfigStore.path(dummyId);
        Resource resource = dataDir.get(path);
        assertFalse(new File(resource.parent().dir(), resource.name()).exists());
    }

    @Test
    public void loadDeprecatedFormat() throws Exception {
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966988";
        String expected = "<RepositoryInfo>"//
                + "<id>" + dummyId + "</id>"//
                + "<parentDirectory>/home/test</parentDirectory>"//
                + "<name>repo</name>"//
                + "</RepositoryInfo>";

        String path = ConfigStore.path(dummyId);
        Resource resource = dataDir.get(path);
        Files.write(expected, resource.file(), Charsets.UTF_8);

        RepositoryInfo info = store.get(dummyId);
        assertNotNull(info);
        assertEquals(dummyId, info.getId());
        assertEquals("file", info.getLocation().getScheme());
        assertTrue(info.getLocation().toString().endsWith("/home/test/repo"));
    }

    @Test
    public void get() throws Exception {
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966988";
        String expected = "<RepositoryInfo>"//
                + "<id>" + dummyId + "</id>"//
                + "<location>file:/home/test/repo</location>"//
                + "</RepositoryInfo>";

        String path = ConfigStore.path(dummyId);
        Resource resource = dataDir.get(path);
        Files.write(expected, resource.file(), Charsets.UTF_8);

        RepositoryInfo info = store.get(dummyId);
        assertNotNull(info);
        assertEquals(dummyId, info.getId());
        assertEquals(new URI("file:/home/test/repo"), info.getLocation());
    }

    @Test
    public void loadMalformed() throws Exception {
        // this xml has a missing > character at the end
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966988";
        String expected = "<RepositoryInfo>"//
                + "<id>" + dummyId + "</id>"//
                + "<parentDirectory>/home/test</parentDirectory>"//
                + "<name>repo</name>"//
                + "</RepositoryInfo";

        String path = ConfigStore.path(dummyId);
        Resource resource = dataDir.get(path);
        Files.write(expected, resource.file(), Charsets.UTF_8);
        thrown.expect(IOException.class);
        thrown.expectMessage("Unable to load");
        store.get(dummyId);
    }

    @Test
    public void getRepositoriesEmpty() {
        List<RepositoryInfo> all = store.getRepositories();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    public void getRepositories() {
        store.save(dummy(1));
        store.save(dummy(2));
        store.save(dummy(3));
        store.save(dummy(4));
        List<RepositoryInfo> all = store.getRepositories();
        assertNotNull(all);
        assertEquals(4, all.size());
        Set<RepositoryInfo> expected = Sets.newHashSet(dummy(1), dummy(2), dummy(3), dummy(4));
        assertEquals(expected, new HashSet<RepositoryInfo>(all));
    }

    @Test
    public void getRepositoriesIgnoresMalformed() throws Exception {
        store.save(dummy(1));
        store.save(dummy(2));
        store.save(dummy(3));
        RepositoryInfo dummy = dummy(4);
        store.save(dummy);
        Resource breakIt = dataDir.get(ConfigStore.path(dummy.getId()));
        byte[] bytes = Files.toByteArray(breakIt.file());
        byte[] from = new byte[bytes.length - 5];
        System.arraycopy(bytes, 0, from, 0, from.length);
        Files.write(from, breakIt.file());

        List<RepositoryInfo> all = store.getRepositories();
        assertNotNull(all);
        assertEquals(3, all.size());
        Set<RepositoryInfo> expected = Sets.newHashSet(dummy(1), dummy(2), dummy(3));
        assertEquals(expected, new HashSet<RepositoryInfo>(all));
    }

    @Test
    public void delete() throws Exception {
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966988";
        String expected = "<RepositoryInfo>"//
                + "<id>" + dummyId + "</id>"//
                + "<parentDirectory>/home/test</parentDirectory>"//
                + "<name>repo</name>"//
                + "</RepositoryInfo>";

        String path = ConfigStore.path(dummyId);
        Resource resource = dataDir.get(path);
        Files.write(expected, resource.file(), Charsets.UTF_8);

        assertNotNull(store.get(dummyId));
        assertTrue(store.delete(dummyId));
        thrown.expect(FileNotFoundException.class);
        assertNull(store.get(dummyId));
    }

    @Test
    public void deleteNull() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("null id");
        store.delete(null);
    }

    @Test
    public void deleteMalformedId() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Id doesn't match UUID format");
        store.delete("not-a-uuid");
    }

    @Test
    public void deleteNonExistent() throws Exception {
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966988";
        assertFalse(store.delete(dummyId));
    }

    private RepositoryInfo dummy(int i) {
        Preconditions.checkArgument(i > -1 && i < 10);
        final String dummyId = "94bcb762-9ee9-4b43-a912-063509966988";
        final String id = dummyId.substring(0, dummyId.length() - 1) + String.valueOf(i);
        RepositoryInfo info = new RepositoryInfo();
        info.setId(id);
        info.setLocation(URI.create("file:/parent/directory/" + i + "/name-" + i));
        return info;
    }
}
