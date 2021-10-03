/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.external.impl.AbstractS3FileServiceImpl;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractS3FileServiceDataTest extends AbstractTaskManagerTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

    /**
     * This test assumes access to aws compatible service.
     *
     * @throws IOException
     */
    @Test
    public void testFileServiceS3CreateSubFolders() throws IOException {
        AbstractS3FileServiceImpl service = getS3FileService();

        String filename = System.currentTimeMillis() + "-test.txt";
        String filenamePath = "new-bucket/New_Folder/" + filename;

        Assert.assertFalse(service.checkFileExists(filenamePath));
        String content = "test the file service";

        // create
        try (InputStream is = IOUtils.toInputStream(content, "UTF-8")) {
            service.create(filenamePath, is);
        }

        // exists
        boolean fileExists = service.checkFileExists(filenamePath);
        Assert.assertTrue(fileExists);

        // read
        try (InputStream is = service.read(filenamePath)) {
            String actualContent = IOUtils.toString(is, "UTF-8");
            Assert.assertEquals(content, actualContent);
        }

        // is create in the root folder?
        Assert.assertTrue(
                service.getS3Client().doesObjectExist(service.getRootFolder(), filenamePath));

        // delete action
        service.delete(filenamePath);
        Assert.assertFalse(service.checkFileExists(filenamePath));
    }

    @Test
    public void testFileServicePrepare() throws IOException, InterruptedException {
        // this test only works in linux because it uses a linux script
        Assume.assumeTrue(SystemUtils.IS_OS_LINUX);

        AbstractS3FileServiceImpl service = getS3FileService();

        // create the script and make executable
        File scriptFile = new File(tempFolder.getRoot(), "prepare.sh");
        try (OutputStream out = new FileOutputStream(scriptFile)) {
            IOUtils.copy(FileServiceDataTest.class.getResourceAsStream("prepare.sh"), out);
        }
        Process p = Runtime.getRuntime().exec("chmod u+x " + scriptFile.getAbsolutePath());
        p.waitFor();
        service.setPrepareScript(scriptFile.getAbsolutePath());

        String filename = System.currentTimeMillis() + "-test.txt";
        String content = "test the file service";
        service.create(filename, IOUtils.toInputStream(content, "UTF-8"), true);

        boolean fileExists = service.checkFileExists(filename);
        Assert.assertTrue(fileExists);

        String actualContent = IOUtils.toString(service.read(filename), "UTF-8");
        // verify extra text!
        Assert.assertEquals(content + "extra text\n", actualContent);

        service.delete(filename);
    }

    @Test
    public void testListSubFoldersS3() throws IOException {
        AbstractS3FileServiceImpl service = getS3FileService();

        service.delete("foo/a.txt");
        service.delete("foo/bar/b.txt");
        service.delete("foo/bar/foobar/barfoo/c.txt");
        service.delete("hello/d.txt");

        try (InputStream content = IOUtils.toInputStream("test the file service", "UTF-8")) {
            service.create("foo/a.txt", content);
        }

        try (InputStream content = IOUtils.toInputStream("test the file service", "UTF-8")) {
            service.create("foo/bar/b.txt", content);
        }

        try (InputStream content = IOUtils.toInputStream("test the file service", "UTF-8")) {
            service.create("foo/bar/foobar/barfoo/c.txt", content);
        }

        try (InputStream content = IOUtils.toInputStream("test the file service", "UTF-8")) {
            service.create("hello/d.txt", content);
        }

        List<String> folders = service.listSubfolders();

        Assert.assertEquals(5, folders.size());
        Assert.assertTrue(folders.contains("foo"));
        Assert.assertTrue(folders.contains("foo/bar"));
        Assert.assertTrue(folders.contains("foo/bar/foobar/barfoo"));
        Assert.assertTrue(folders.contains("foo/bar/foobar"));
        Assert.assertTrue(folders.contains("hello"));

        service.delete("foo/a.txt");
        service.delete("foo/bar/b.txt");
        service.delete("foo/bar/foobar/barfoo/c.txt");
        service.delete("hello/d.txt");
    }

    protected abstract AbstractS3FileServiceImpl getS3FileService();
}
