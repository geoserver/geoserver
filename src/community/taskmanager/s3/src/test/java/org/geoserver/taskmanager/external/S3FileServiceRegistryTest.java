/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import java.util.Arrays;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.external.impl.AWSFileServiceImpl;
import org.geoserver.taskmanager.external.impl.FileServiceImpl;
import org.geoserver.taskmanager.external.impl.S3FileServiceImpl;
import org.geoserver.taskmanager.util.LookupService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class S3FileServiceRegistryTest extends AbstractTaskManagerTest {

    @Autowired public LookupService<FileService> fileServiceRegistry;

    @Test
    public void testFileRegistry() {
        Assert.assertEquals(6, fileServiceRegistry.names().size());

        FileService fs = fileServiceRegistry.get("s3-test-source");
        Assert.assertNotNull(fs);
        Assert.assertTrue(fs instanceof S3FileServiceImpl);
        Assert.assertEquals("http://127.0.0.1:9000", ((S3FileServiceImpl) fs).getEndpoint());
        Assert.assertEquals("source", ((S3FileServiceImpl) fs).getRootFolder());
        Assert.assertEquals(Arrays.asList("a", "b", "c"), ((S3FileServiceImpl) fs).getRoles());

        fs = fileServiceRegistry.get("s3-test-target");
        Assert.assertNotNull(fs);
        Assert.assertTrue(fs instanceof S3FileServiceImpl);
        Assert.assertEquals("http://127.0.0.1:9000", ((S3FileServiceImpl) fs).getEndpoint());
        Assert.assertEquals("target", ((S3FileServiceImpl) fs).getRootFolder());
        Assert.assertEquals(Arrays.asList("d", "e", "f"), ((S3FileServiceImpl) fs).getRoles());

        fs = fileServiceRegistry.get("aws-taskmanagertest");
        Assert.assertNotNull(fs);
        Assert.assertTrue(fs instanceof AWSFileServiceImpl);
        Assert.assertEquals("taskmanagertest", ((AWSFileServiceImpl) fs).getRootFolder());

        fs = fileServiceRegistry.get("temp-directory");
        Assert.assertNotNull(fs);
        Assert.assertTrue(fs instanceof FileServiceImpl);
        Assert.assertEquals("/tmp", ((FileServiceImpl) fs).getRootFolder());
    }
}
