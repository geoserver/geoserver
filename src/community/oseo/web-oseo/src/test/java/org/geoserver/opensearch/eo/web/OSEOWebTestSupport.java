/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OSEOTestSupport;
import org.geoserver.web.GeoServerWicketTestSupport;

/**
 * Support class for writing UI tests
 *
 * @author Andrea Aime - GeoSolutions
 */
public class OSEOWebTestSupport extends GeoServerWicketTestSupport {

    String openSearchAccessStoreId;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data to setup
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        OSEOTestSupport.setupBasicOpenSearch(testData, getCatalog(), getGeoServer(), false);
        OSEOInfo service = getGeoServer().getService(OSEOInfo.class);
        openSearchAccessStoreId = service.getOpenSearchAccessStoreId();
    }
}
