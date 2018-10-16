/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.external.impl.FileServiceImpl;
import org.geoserver.taskmanager.util.LookupService;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test data methods.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class FileServiceDataTest extends AbstractTaskManagerTest {

    @Autowired LookupService<FileService> fileServiceRegistry;

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testFileRegistry() {
        Assert.assertEquals(2, fileServiceRegistry.names().size());

        FileService fs = fileServiceRegistry.get("temp-directory");
        Assert.assertNotNull(fs);
        Assert.assertTrue(fs instanceof FileServiceImpl);
        Assert.assertEquals("/tmp", ((FileServiceImpl) fs).getRootFolder());
    }

    @Test
    public void testFileService() throws IOException {
        FileServiceImpl service = new FileServiceImpl();
        service.setRootFolder(FileUtils.getTempDirectoryPath());

        String filename = System.currentTimeMillis() + "-test.txt";
        String path = FileUtils.getTempDirectoryPath() + "/" + filename;

        Assert.assertFalse(Files.exists(Paths.get(path)));
        String content = "test the file service";
        service.create(filename, IOUtils.toInputStream(content, "UTF-8"));
        Assert.assertTrue(Files.exists(Paths.get(path)));

        boolean fileExists = service.checkFileExists(filename);
        Assert.assertTrue(fileExists);

        String actualContent = IOUtils.toString(service.read(filename));
        Assert.assertEquals(content, actualContent);

        service.delete(filename);
        Assert.assertFalse(Files.exists(Paths.get(path)));
    }

    @Test
    public void testFileServicePrepare() throws IOException, InterruptedException {
        // this test only works in linux because it uses a linux script
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);

        FileServiceImpl service = new FileServiceImpl();
        service.setRootFolder(FileUtils.getTempDirectoryPath());

        // create the script and make executable
        File scriptFile = new File(tempFolder.getRoot(), "prepare.sh");
        try (OutputStream out = new FileOutputStream(scriptFile)) {
            IOUtils.copy(FileServiceDataTest.class.getResourceAsStream("prepare.sh"), out);
        }
        Process p = Runtime.getRuntime().exec("chmod u+x " + scriptFile.getAbsolutePath());
        p.waitFor();
        service.setPrepareScript(scriptFile.getAbsolutePath());

        String filename = System.currentTimeMillis() + "-test.txt";
        String path = FileUtils.getTempDirectoryPath() + "/" + filename;

        Assert.assertFalse(Files.exists(Paths.get(path)));
        String content = "test the file service";
        service.create(filename, IOUtils.toInputStream(content, "UTF-8"), true);
        Assert.assertTrue(Files.exists(Paths.get(path)));

        boolean fileExists = service.checkFileExists(filename);
        Assert.assertTrue(fileExists);

        String actualContent = IOUtils.toString(service.read(filename));
        // verify extra text!
        Assert.assertEquals(content + "extra text\n", actualContent);

        service.delete(filename);
        Assert.assertFalse(Files.exists(Paths.get(path)));
    }

    @Test
    public void testFileServiceCreateSubFolders() throws IOException {
        FileServiceImpl service = new FileServiceImpl();
        service.setRootFolder(FileUtils.getTempDirectoryPath() + "/fileservicetest/");

        String filename = "subfolder/" + System.currentTimeMillis() + "-test.txt";
        String path = FileUtils.getTempDirectoryPath() + "/fileservicetest/" + filename;

        Assert.assertFalse(Files.exists(Paths.get(path)));
        service.create(filename, IOUtils.toInputStream("test the file service", "UTF-8"));
        Assert.assertTrue(Files.exists(Paths.get(path)));

        boolean fileExists = service.checkFileExists(filename);
        Assert.assertTrue(fileExists);

        service.delete(filename);
        Assert.assertFalse(Files.exists(Paths.get(path)));

        List<String> folders = service.listSubfolders();
        Assert.assertEquals(1, folders.size());
    }

    @Test
    public void testListSubFolders() throws IOException {
        FileServiceImpl service = new FileServiceImpl();
        service.setRootFolder(
                FileUtils.getTempDirectoryPath() + "/folder-" + System.currentTimeMillis() + "/");

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

    @Test
    public void testVersionedPath() {
        assertEquals("myfile.###", FileService.versioned("myfile"));
        assertEquals("myfile.###.ext", FileService.versioned("myfile.ext"));
        assertEquals("/path/to/myfile.###.ext", FileService.versioned("/path/to/myfile.ext"));
        assertEquals("my###file.ext", FileService.versioned("my###file.ext"));
    }
}
