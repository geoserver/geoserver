/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.DataAccessManager;
import org.geoserver.security.DataAccessManagerAdapter;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.springframework.security.core.Authentication;

/**
 * Similar to the GetFeatureInfoTest this class runs tests the GetFeatureInfo request for WMS
 * layers. What makes these unit tests unique is that they apply read/write restrictions.
 *
 * @author Josh Vote, CSIRO Earth Science and Resource Engineering
 */
public class GetFeatureInfoRestrictedTest extends WMSTestSupport {

    /**
     * Simple extension of org.geoserver.security.SecureCatalogImpl that exposes the constructor
     * that includes a org.geoserver.security.DataAccessManager
     */
    class TestableSecureCatalogImpl extends SecureCatalogImpl {
        public TestableSecureCatalogImpl(Catalog catalog, DataAccessManager manager) {
            super(catalog, new DataAccessManagerAdapter(manager));
        }
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // Create a mock access manager that will grant read but deny write access to everyone
        DataAccessManager mockManager = createMock(DataAccessManager.class);
        expect(
                        mockManager.canAccess(
                                (Authentication) anyObject(),
                                (WorkspaceInfo) anyObject(),
                                eq(AccessMode.READ)))
                .andReturn(true)
                .anyTimes();
        expect(
                        mockManager.canAccess(
                                (Authentication) anyObject(),
                                (WorkspaceInfo) anyObject(),
                                eq(AccessMode.WRITE)))
                .andReturn(false)
                .anyTimes();
        expect(
                        mockManager.canAccess(
                                (Authentication) anyObject(),
                                (WorkspaceInfo) anyObject(),
                                eq(AccessMode.ADMIN)))
                .andReturn(false)
                .anyTimes();
        expect(
                        mockManager.canAccess(
                                (Authentication) anyObject(),
                                (ResourceInfo) anyObject(),
                                eq(AccessMode.READ)))
                .andReturn(true)
                .anyTimes();
        expect(
                        mockManager.canAccess(
                                (Authentication) anyObject(),
                                (ResourceInfo) anyObject(),
                                eq(AccessMode.WRITE)))
                .andReturn(false)
                .anyTimes();
        expect(
                        mockManager.canAccess(
                                (Authentication) anyObject(),
                                (LayerInfo) anyObject(),
                                eq(AccessMode.READ)))
                .andReturn(true)
                .anyTimes();
        expect(
                        mockManager.canAccess(
                                (Authentication) anyObject(),
                                (LayerInfo) anyObject(),
                                eq(AccessMode.WRITE)))
                .andReturn(false)
                .anyTimes();
        expect(mockManager.getMode()).andReturn(CatalogMode.HIDE).anyTimes();
        replay(mockManager);

        // Overwrite our catalog with this new restricted catalog
        getGeoServer()
                .setCatalog(
                        new TestableSecureCatalogImpl(getGeoServer().getCatalog(), mockManager));
    }

    /** Test the effects of reprojection on a readonly layer (Created to expose GEOS-3977) */
    @Test
    public void testRestrictedReprojection() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?SERVICE=WMS&REQUEST=GetFeatureInfo&EXCEPTIONS=application/vnd.ogc.se_xml&VERSION=1.1.1&BBOX=-0.002,-0.002,0.002,0.002&X=109&Y=204&INFO_FORMAT=text/html&QUERY_LAYERS="
                        + layer
                        + "&FEATURE_COUNT=50&SRS=EPSG:4326&LAYERS="
                        + layer
                        + "&STYLES=&WIDTH=256&HEIGHT=256&FORMAT=image/png";
        String result = getAsString(request);

        // System.out.println(result);

        assertNotNull(result);
        assertTrue(result.indexOf("ServiceExceptionReport") < 0);
        assertTrue(result.indexOf("Green Forest") > 0);
    }
}
