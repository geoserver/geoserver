/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.geoserver.opensearch.eo.store.GeoServerOpenSearchTestSupport.setupBasicOpenSearch;

import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.store.GeoServerOpenSearchTestSupport;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccessTest;
import org.junit.BeforeClass;

public class STACTestSupport extends OGCApiTestSupport {
    protected static final String STAC_TITLE = "STAC server title";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServer gs = getGeoServer();
        OSEOInfo service = gs.getService(OSEOInfo.class);
        service.setTitle(STAC_TITLE);
        gs.save(service);

        setupBasicOpenSearch(testData, getCatalog(), gs, false);

        // add the custom product class
        service.getProductClasses().add(JDBCOpenSearchAccessTest.GS_PRODUCT);
        gs.save(service);
    }

    @BeforeClass
    public static void checkOnLine() {
        GeoServerOpenSearchTestSupport.checkOnLine();
    }
}
