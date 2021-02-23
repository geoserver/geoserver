/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.taskmanager.external.impl.S3FileServiceImpl;
import org.geotools.util.logging.Logging;
import org.junit.Assume;

/**
 * Test data methods.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class S3FileServiceDataTest extends AbstractS3FileServiceDataTest {

    private static final Logger LOGGER = Logging.getLogger(S3FileServiceDataTest.class);

    @Override
    protected S3FileServiceImpl getS3FileService() {
        S3FileServiceImpl s3FileService =
                new S3FileServiceImpl(
                        "http://127.0.0.1:9000",
                        "P3Z48TR2OZAZDP8C3P9E",
                        "sCNEAhfGtlhA8Mjq1AReBcMl0oMGX1zE3vppQRXB",
                        "alias",
                        "source");
        List<String> folders = null;
        try {
            folders = s3FileService.listSubfolders();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        Assume.assumeNotNull(folders);

        return s3FileService;
    }
}
