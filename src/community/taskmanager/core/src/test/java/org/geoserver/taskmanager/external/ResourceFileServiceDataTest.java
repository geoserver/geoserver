/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.external.impl.FileServiceImpl;
import org.geoserver.taskmanager.external.impl.ResourceFileServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test data methods.
 *
 * @author Niels Charlier
 */
public class ResourceFileServiceDataTest extends AbstractTaskManagerTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    public ResourceStore resourceStore;

    @Before
    public void before() {
        resourceStore = new FileSystemResourceStore(tempFolder.getRoot());
    }

    @Test
    public void testFileService() throws IOException {
        ResourceFileServiceImpl service = new ResourceFileServiceImpl(resourceStore);
        service.setRootFolder("/tmp");

        String filename = System.currentTimeMillis() + "-test.txt";
        Resource res = resourceStore.get("/tmp/" + filename);

        Assert.assertFalse(Resources.exists(res));
        String content = "test the file service";
        service.create(filename, IOUtils.toInputStream(content, "UTF-8"));
        Assert.assertTrue(Resources.exists(res));

        boolean fileExists = service.checkFileExists(filename);
        Assert.assertTrue(fileExists);

        String actualContent = IOUtils.toString(service.read(filename), "UTF-8");
        Assert.assertEquals(content, actualContent);

        service.delete(filename);
        Assert.assertFalse(Resources.exists(res));
    }

    @Test
    public void testFileServiceCreateSubFolders() throws IOException {
        ResourceFileServiceImpl service = new ResourceFileServiceImpl(resourceStore);
        service.setRootFolder("/tmp");

        String filename = "subfolder/" + System.currentTimeMillis() + "-test.txt";
        Resource res = resourceStore.get("/tmp/" + filename);

        Assert.assertFalse(Resources.exists(res));
        service.create(filename, IOUtils.toInputStream("test the file service", "UTF-8"));
        Assert.assertTrue(Resources.exists(res));

        boolean fileExists = service.checkFileExists(filename);
        Assert.assertTrue(fileExists);

        service.delete(filename);
        Assert.assertFalse(Resources.exists(res));

        List<String> folders = service.listSubfolders();
        Assert.assertEquals(1, folders.size());
    }

    @Test
    public void testFileServiceGetVersioned() throws IOException {
        new FileOutputStream(new File(tempFolder.getRoot(), "test.6.txt")).close();

        ResourceFileServiceImpl service = new ResourceFileServiceImpl(resourceStore);

        FileReference ref = service.getVersioned("test.###.txt");
        assertEquals("test.6.txt", ref.getLatestVersion());
        assertEquals("test.7.txt", ref.getNextVersion());
    }

    @Test
    public void testListSubFolders() throws IOException {
        FileServiceImpl service = new FileServiceImpl();
        service.setRootFolder("/tmp/folder-" + System.currentTimeMillis() + "/");

        InputStream content = IOUtils.toInputStream("test the file service", "UTF-8");

        service.create("foo/a.txt", content);
        service.create("foo/bar/b.txt", content);
        service.create("foo/bar/foobar/barfoo/c.txt", content);
        service.create("hello/d.txt", content);
        service.create("e.txt", content);
        service.create("f.txt", content);

        List<String> folders = service.listSubfolders();

        Assert.assertEquals(5, folders.size());
        Assert.assertTrue(folders.contains("foo"));
        Assert.assertTrue(folders.contains(Paths.get("foo", "bar").toString()));
        Assert.assertTrue(folders.contains(Paths.get("foo", "bar", "foobar").toString()));
        Assert.assertTrue(folders.contains(Paths.get("foo", "bar", "foobar", "barfoo").toString()));
        Assert.assertTrue(folders.contains("hello"));
    }
}
