/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import java.util.Arrays;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCApiTestSupport;

public class CoveragesTestSupport extends OGCApiTestSupport {

    protected static final String TAZDEM_TITLE = "Down under digital elevation model";
    protected static final String TAZDEM_DESCRIPTION = "So that you know where up and down are";
    protected static final QName TIMESERIES =
            new QName(MockData.SF_URI, "timeseries", MockData.SF_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // rename workspace to avoid https://osgeo-org.atlassian.net/browse/GEOS-10155
        // which does seem to require an API break to be solved
        Catalog catalog = getCatalog();
        WorkspaceInfo wcs = catalog.getWorkspaceByName("wcs");
        wcs.setName("rs");
        catalog.save(wcs);

        // customize metadata and set custom CRS too
        CoverageInfo tazDem = getCatalog().getCoverageByName("rs:DEM");
        tazDem.setTitle(TAZDEM_TITLE);
        tazDem.setAbstract(TAZDEM_DESCRIPTION);
        tazDem.getResponseSRS().addAll(Arrays.asList("3857", "3003"));
        getCatalog().save(tazDem);

        // add temporal layer
        testData.addRasterLayer(TIMESERIES, "timeseries.zip", null, getCatalog());
    }
}
