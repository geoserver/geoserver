/* (c) 2013-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.junit.Assume;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.spatial.Intersects;
import org.springframework.security.core.Authentication;

public class AccessManagerTest extends GeofenceBaseTest {

    @Test
    public void testAdmin() {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        Authentication user = getUser("admin", "geoserver", "ROLE_ADMINISTRATOR");

        login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX); // uses the login

        // check workspace access
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());

        // check layer access
        LayerInfo layer =
                catalog.getLayerByName(getLayerId(MockData.BASIC_POLYGONS)); // uses the login

        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, layer);
        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
        assertNull(vl.getReadAttributes());
        assertNull(vl.getWriteAttributes());
    }

    @Test
    public void testCiteCannotWriteOnWorkspace() {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        configManager.getConfiguration().setGrantWriteToWorkspacesToAuthenticatedUsers(false);
        Authentication user = getUser("cite", "cite", "ROLE_AUTHENTICATED");

        // check workspace access
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertFalse(wl.isWritable());
    }

    @Test
    public void testCiteCanWriteOnWorkspace() {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        configManager.getConfiguration().setGrantWriteToWorkspacesToAuthenticatedUsers(true);

        Authentication user = getUser("cite", "cite", "ROLE_AUTHENTICATED");

        // check workspace access
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());
        configManager.getConfiguration().setGrantWriteToWorkspacesToAuthenticatedUsers(false);
    }

    @Test
    public void testAnonymousUser() {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        // check workspace access
        // WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        // WorkspaceAccessLimits wl = manager.getAccessLimits(null, citeWS);
        // assertFalse(wl.isReadable());
        // assertFalse(wl.isWritable());

        // setup layer
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        LayerInfo layer = catalog.getLayerByName(getLayerId(MockData.BASIC_POLYGONS));

        // check layer access
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(null, layer);
        assertEquals(Filter.EXCLUDE, vl.getReadFilter());
        assertEquals(Filter.EXCLUDE, vl.getWriteFilter());
        assertNull(vl.getReadAttributes());
        assertNull(vl.getWriteAttributes());
    }

    @Test
    public void testCiteWorkspaceAccess() {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        Authentication user = getUser("cite", "cite", "ROLE_AUTHENTICATED");

        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        // check workspace access on cite
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertFalse(wl.isWritable());

        // check workspace access on any other but not cite and sf (should fail)
        WorkspaceInfo cdfWS = catalog.getWorkspaceByName(MockData.CDF_PREFIX);
        wl = accessManager.getAccessLimits(user, cdfWS);
        assertTrue(wl.isReadable());
        assertFalse(wl.isWritable());

        // check workspace access on sf (should work, we can do at least a getmap)
        WorkspaceInfo sfWS = catalog.getWorkspaceByName(MockData.SF_PREFIX);
        wl = accessManager.getAccessLimits(user, sfWS);
        assertTrue(wl.isReadable());
        assertFalse(wl.isWritable());
    }

    @Test
    public void testCiteLayerAccess() {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        Authentication user = getUser("cite", "cite", "ROLE_AUTHENTICATED");

        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

        // check layer in the cite workspace
        LayerInfo bpolygons = catalog.getLayerByName(getLayerId(MockData.BASIC_POLYGONS));
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, bpolygons);
        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
        assertNull(vl.getReadAttributes());
        assertNull(vl.getWriteAttributes());

        // check layer in the sf workspace with a wfs request
        Request request = new Request();
        request.setService("WFS");
        request.setRequest("GetFeature");
        Dispatcher.REQUEST.set(request);

        LayerInfo generic = catalog.getLayerByName(getLayerId(MockData.GENERICENTITY));
        vl = (VectorAccessLimits) accessManager.getAccessLimits(user, generic);
        assertEquals(Filter.EXCLUDE, vl.getReadFilter());
        assertEquals(Filter.EXCLUDE, vl.getWriteFilter());

        // now fake a getmap request (using a service and request with a different case than the
        // geofenceService)
        request = new Request();
        request.setService("WmS");
        request.setRequest("gETmAP");
        Dispatcher.REQUEST.set(request);
        vl = (VectorAccessLimits) accessManager.getAccessLimits(user, generic);
        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
    }

    @Test
    public void testWmsLimited() {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        Authentication user = getUser("wmsuser", "wmsuser", "ROLE_AUTHENTICATED");

        // check layer in the sf workspace with a wfs request
        Request request = new Request();
        request.setService("WFS");
        request.setRequest("GetFeature");
        Dispatcher.REQUEST.set(request);

        LayerInfo generic = catalog.getLayerByName(getLayerId(MockData.GENERICENTITY));
        if (generic != null) {
            VectorAccessLimits vl =
                    (VectorAccessLimits) accessManager.getAccessLimits(user, generic);
            assertEquals(Filter.INCLUDE, vl.getReadFilter());
            assertEquals(Filter.INCLUDE, vl.getWriteFilter());

            // now fake a getmap request (using a service and request with a different case than the
            // geofenceService)
            request = new Request();
            request.setService("wms");
            Dispatcher.REQUEST.set(request);
            vl = (VectorAccessLimits) accessManager.getAccessLimits(user, generic);
            assertEquals(Filter.INCLUDE, vl.getReadFilter());
            assertEquals(Filter.INCLUDE, vl.getWriteFilter());
        }
    }

    @Test
    public void testAreaLimited() throws Exception {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        Authentication user = getUser("area", "area", "ROLE_AUTHENTICATED");
        login("area", "area", "ROLE_AUTHENTICATED");

        // check we have the geometry filter set
        LayerInfo generic = catalog.getLayerByName(getLayerId(MockData.GENERICENTITY));
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, generic);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        Geometry limit =
                new WKTReader().read("MULTIPOLYGON(((48 62, 48 63, 49 63, 49 62, 48 62)))");
        Filter filter = ff.intersects(ff.property(""), ff.literal(limit));

        assertEquals(filter, vl.getReadFilter());
        assertEquals(filter, vl.getWriteFilter());
    }

    /**
     * This test is very similar to testAreaLimited(), but the source resource is set to have the
     * 900913 SRS. We expect that the allowedarea is projected into the resource CRS.
     */
    @Test
    public void testArea900913Vector() throws Exception {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        Authentication user = getUser("area", "area", "ROLE_AUTHENTICATED");
        login("area", "area", "ROLE_AUTHENTICATED");

        LayerInfo generic = catalog.getLayerByName(getLayerId(MockData.GENERICENTITY));

        // Create a layer using as much as info from the Mock instance, making sure we're declaring
        // the 900913 SRS.
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setName(generic.getResource().getStore().getWorkspace().getName());

        StoreInfo store = new DataStoreInfoImpl(catalog);
        store.setWorkspace(ws);

        FeatureTypeInfoImpl resource = new FeatureTypeInfoImpl(catalog);
        resource.setNamespace(generic.getResource().getNamespace());
        resource.setSRS("EPSG:900913");
        resource.setName(generic.getResource().getName());
        resource.setStore(store);

        LayerInfoImpl layerInfo = new LayerInfoImpl();
        layerInfo.setResource(resource);
        layerInfo.setName(generic.getName());

        // Check we have the geometry filter set
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, resource);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        Geometry expectedLimit =
                new WKTReader()
                        .read(
                                " MULTIPOLYGON (((5343335.558077131 8859142.800565697, 5343335.558077131 9100250.907059547, 5454655.048870404 9100250.907059547, 5454655.048870404 8859142.800565697, 5343335.558077131 8859142.800565697)))");
        Filter filter = ff.intersects(ff.property(""), ff.literal(expectedLimit));

        IntersectExtractor ier = new IntersectExtractor();
        vl.getReadFilter().accept(ier, null);
        assertTrue(expectedLimit.equalsExact(ier.geom, .000000001));

        IntersectExtractor iew = new IntersectExtractor();
        vl.getWriteFilter().accept(iew, null);
        assertTrue(expectedLimit.equalsExact(iew.geom, .000000001));
    }

    @Test
    public void testArea900913Raster() throws Exception {
        Assume.assumeTrue(IS_GEOFENCE_AVAILABLE);

        Authentication user = getUser("area", "area", "ROLE_AUTHENTICATED");
        login("area", "area", "ROLE_AUTHENTICATED");

        LayerInfo generic = catalog.getLayerByName(getLayerId(MockData.GENERICENTITY));

        // Create a layer using as much as info from the Mock instance, making sure we're declaring
        // the 900913 SRS.
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setName(generic.getResource().getStore().getWorkspace().getName());

        StoreInfo store = new CoverageStoreInfoImpl(catalog);
        store.setWorkspace(ws);

        CoverageInfoImpl resource = new CoverageInfoImpl(catalog);
        resource.setNamespace(generic.getResource().getNamespace());
        resource.setSRS("EPSG:900913");
        resource.setName(generic.getResource().getName());
        resource.setStore(store);

        LayerInfoImpl layerInfo = new LayerInfoImpl();
        layerInfo.setResource(resource);
        layerInfo.setName(generic.getName());

        // Check we have the geometry filter set
        CoverageAccessLimits accessLimits =
                (CoverageAccessLimits) accessManager.getAccessLimits(user, resource);

        Geometry expectedLimit =
                new WKTReader()
                        .read(
                                "MULTIPOLYGON (((5343335.558077131 8859142.800565697, 5343335.558077131 9100250.907059547, 5454655.048870404 9100250.907059547, 5454655.048870404 8859142.800565697, 5343335.558077131 8859142.800565697)))");

        assertTrue(expectedLimit.equalsExact(accessLimits.getRasterFilter(), .000000001));
    }

    static class IntersectExtractor extends DefaultFilterVisitor {

        Geometry geom;

        @Override
        public Object visit(Intersects filter, Object data) {
            geom = (Geometry) filter.getExpression2().evaluate(null);
            return data;
        }
    }
}
