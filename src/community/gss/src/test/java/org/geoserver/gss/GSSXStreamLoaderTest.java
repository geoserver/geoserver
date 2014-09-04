/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.gss.GSSInfo.GSSMode;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataAccess;

public class GSSXStreamLoaderTest extends GSSTestSupport {

    /**
     * Saving and reloading the GSS service configuration was not working, this test ensure that it
     * actually does
     */
    public void testReload() throws Exception {
        GeoServerLoader loader = GeoServerExtensions.bean(GeoServerLoader.class);
        getCatalog().getResourcePool().dispose();
        loader.reload();

        GSSInfo info = getGeoServer().getService(GSSInfo.class);

        assertNotNull(info);

        assertEquals("GSS", info.getName());
        assertEquals(GSSMode.Unit, info.getMode());
        DataAccess storeCatalog = getCatalog().getStore("synchStore", DataStoreInfo.class)
                .getDataStore(null);
        DataAccess storeService = info.getVersioningDataStore().getDataStore(null);
        assertSame(storeCatalog, storeService);
    }

}
