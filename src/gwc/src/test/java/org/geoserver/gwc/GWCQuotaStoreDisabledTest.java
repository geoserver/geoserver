/* (c) 2014 -2024 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.junit.After;
import org.junit.Test;

public class GWCQuotaStoreDisabledTest extends GeoServerSystemTestSupport {

    /**
     * We use this call because we need to set the system property before the app context gets loaded, and we don't need
     * any test data
     */
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // disable disk quota completely
        System.setProperty(DiskQuotaMonitor.GWC_DISKQUOTA_DISABLED, "true");
        testData.setUpSecurity();
    }

    @After
    public void resetSystemProperties() throws Exception {
        System.clearProperty(DiskQuotaMonitor.GWC_DISKQUOTA_DISABLED);
    }

    @Test
    public void testQuotaDisabled() throws Exception {
        // the provider returns no quota store
        ConfigurableQuotaStoreProvider provider = GeoServerExtensions.bean(ConfigurableQuotaStoreProvider.class);
        assertNull(provider.getQuotaStore());

        // check there is no quota database
        File hsqlQuotaStore = new File("diskquota_page_store_hsql");
        assertFalse(hsqlQuotaStore.exists());
    }

    @Test
    public void testQuotaDisabledOnDestroy() throws Exception {
        ConfigurableQuotaStoreProvider provider = GeoServerExtensions.bean(ConfigurableQuotaStoreProvider.class);

        // check that no NPE is thrown on destroy() (because the store is null)
        try {
            provider.destroy();
        } catch (NullPointerException e) {
            fail("NullPointerException was thrown when destroying ConfigurableQuotaStoreProvider");
        }
    }
}
