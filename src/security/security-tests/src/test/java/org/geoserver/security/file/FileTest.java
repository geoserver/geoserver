/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.file;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.awaitility.Awaitility;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.AbstractRoleService;
import org.geoserver.security.impl.AbstractUserGroupService;
import org.junit.Test;

/** @author christian */
public class FileTest {
    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");
    int gaCounter = 0, ugCounter = 0;

    GeoServerRoleService gaService =
            new AbstractRoleService() {

                @Override
                public String getName() {
                    return "TestGAService";
                };

                @Override
                protected void deserialize() throws IOException {
                    gaCounter++;
                }

                @Override
                public void initializeFromConfig(SecurityNamedServiceConfig config)
                        throws IOException {
                    super.initializeFromConfig(config);
                }
            };

    GeoServerUserGroupService ugService =
            new AbstractUserGroupService() {

                @Override
                public String getName() {
                    return "TestUGService";
                };

                @Override
                protected void deserialize() throws IOException {
                    ugCounter++;
                }

                @Override
                public void initializeFromConfig(SecurityNamedServiceConfig config)
                        throws IOException {}
            };

    @Test
    public void testFileWatcher() throws Exception {
        Files.schedule(100, TimeUnit.MILLISECONDS);
        try {
            File ugFile = File.createTempFile("users", ".xml");
            ugFile.deleteOnExit();
            File gaFile = File.createTempFile("roles", ".xml");
            gaFile.deleteOnExit();

            RoleFileWatcher gaWatcher = new RoleFileWatcher(Files.asResource(gaFile), gaService);
            assertEquals(1, gaCounter);

            gaWatcher.start();

            UserGroupFileWatcher ugWatcher =
                    new UserGroupFileWatcher(Files.asResource(ugFile), ugService);
            assertEquals(1, ugCounter);
            ugWatcher.start();

            LOGGER.info(gaWatcher.toString());
            LOGGER.info(ugWatcher.toString());

            // now, modifiy last access
            ugFile.setLastModified(ugFile.lastModified() + 1000);
            gaFile.setLastModified(gaFile.lastModified() + 1000);

            Awaitility.await().atMost(2, SECONDS).until(() -> ugCounter == 2 && gaCounter == 2);
            ugWatcher.setTerminate(true);
            gaWatcher.setTerminate(true);
            ugFile.delete();
            gaFile.delete();
        } finally {
            Files.schedule(10, SECONDS);
        }
    }
}
