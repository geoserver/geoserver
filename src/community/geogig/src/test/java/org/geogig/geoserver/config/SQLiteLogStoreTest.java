/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.ResourceStore;
import org.junit.Test;

public class SQLiteLogStoreTest extends AbstractLogStoreTest {

    @Override
    protected ResourceStore getResourceStore() {
        return new FileSystemResourceStore(tmpDir.getRoot());
    }

    @Override
    protected void populateConfigProperties(Properties properties) {
        // do nothing create the default log store.
    }

    @Test
    public void testCreatesDefault() throws Exception {
        File file =
                FileUtils.getFile(
                        tmpDir.getRoot(), "geogig", "config", "security", "securitylogs.db");
        logStore.afterPropertiesSet();
        assertTrue(file.exists());
    }
}
