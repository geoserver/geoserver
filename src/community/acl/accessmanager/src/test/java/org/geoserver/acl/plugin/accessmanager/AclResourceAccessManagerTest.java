/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geolatte.geom.MultiPolygon;
import org.geolatte.geom.codec.Wkt;
import org.geoserver.acl.domain.rules.InsertPosition;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.acl.domain.rules.RuleLimits;
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
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AclResourceAccessManagerTest extends AclGeoServerSystemTestSupport {

    @Test
    public void testAdmin() {
        Authentication user = getUser("admin", "geoserver", "ROLE_ADMINISTRATOR");

        login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX); // uses the login

        // check workspace access
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());

        // check layer access
        LayerInfo layer = catalog.getLayerByName(getLayerId(MockData.BASIC_POLYGONS)); // uses the login

        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, layer);
        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
        assertNull(vl.getReadAttributes());
        assertNull(vl.getWriteAttributes());
    }

    @Test
    public void testCiteCannotWriteOnWorkspace() {
        accessManager.setGrantWriteToWorkspacesToAuthenticatedUsers(false);
        Authentication user = getUser("cite", "cite", "ROLE_AUTHENTICATED");

        // check workspace access
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertFalse(wl.isWritable());
    }

    @Test
    public void testCiteCanWriteOnWorkspace() {
        accessManager.setGrantWriteToWorkspacesToAuthenticatedUsers(true);

        Authentication user = getUser("cite", "cite", "ROLE_AUTHENTICATED");

        // check workspace access
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());
        accessManager.setGrantWriteToWorkspacesToAuthenticatedUsers(false);
    }

    @Test
    public void testAnonymousUser() {
        // login as admin so we can get the layer from the secure catalog
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        LayerInfo layer = catalog.getLayerByName(getLayerId(MockData.BASIC_POLYGONS));
        assertNotNull(layer);
        logout();
        // check layer access for anonymous
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(null, layer);
        assertEquals(Filter.EXCLUDE, vl.getReadFilter());
        assertEquals(Filter.EXCLUDE, vl.getWriteFilter());
        assertNull(vl.getReadAttributes());
        assertNull(vl.getWriteAttributes());
    }

    @Test
    public void testCiteWorkspaceAccess() {
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
        Authentication user = getUser("cite", "cite", "ROLE_AUTHENTICATED");
        // let the cite user see the cite workspace
        ruleAdminService.insert(Rule.allow().withUsername("cite").withWorkspace("cite"), InsertPosition.FROM_START);

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

        request = new Request();
        request.setService("WmS"); // case shouldn't matter
        request.setRequest("gETmAP"); // case shouldn't matter
        Dispatcher.REQUEST.set(request);
        vl = (VectorAccessLimits) accessManager.getAccessLimits(user, generic);
        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
    }

    /**
     * Catalog is a {@link SecureCatalogImpl}, and the base test sets up a rule where everyone can access the WMS
     * service, but not any other service. Test the catalog hides layers for a WFS request, but are visible to WMS
     * requests
     */
    @Test
    public void testWmsLimited() {
        Authentication user = getUser("wmsuser", "wmsuser", "ROLE_AUTHENTICATED");
        SecurityContextHolder.getContext().setAuthentication(user);

        // check layer in the sf workspace with a wfs request
        Request request = new Request();
        request.setService("WFS");
        request.setRequest("GetFeature");
        Dispatcher.REQUEST.set(request);

        LayerInfo generic = catalog.getLayerByName(getLayerId(MockData.GENERICENTITY));
        assertNull("Layer should not be visible to WFS as per the default rules set up in AclTestBase", generic);

        request = new Request();
        request.setService("WMS");
        request.setRequest("GetMap");
        Dispatcher.REQUEST.set(request);

        generic = catalog.getLayerByName(getLayerId(MockData.GENERICENTITY));
        assertNotNull("Layer should be visible to WMS as per the default rules set up in AclTestBase", generic);

        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, generic);
        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());

        request = new Request();
        request.setService("wms"); // case shouldn't matter
        Dispatcher.REQUEST.set(request);
        vl = (VectorAccessLimits) accessManager.getAccessLimits(user, generic);
        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
    }

    @Test
    public void testAreaLimited() throws Exception {
        Authentication user = getUser("area", "area", "ROLE_AUTHENTICATED");
        login("area", "area", "ROLE_AUTHENTICATED");
        // let the area user see the sf workspace
        Rule rule = ruleAdminService.insert(
                Rule.limit().withPriority(1).withUsername("area").withWorkspace("sf"));
        ruleAdminService.insert(
                Rule.allow().withPriority(2).withUsername("area").withWorkspace("sf"));
        ruleAdminService.setLimits(
                rule.getId(),
                RuleLimits.builder()
                        .allowedArea((MultiPolygon<?>)
                                Wkt.fromWkt("SRID=4326;MULTIPOLYGON(((48 62, 48 63, 49 63, 49 62, 48 62)))"))
                        .build());

        // check we have the geometry filter set
        LayerInfo generic = catalog.getLayerByName(getLayerId(MockData.GENERICENTITY));
        assertNotNull(generic);
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, generic);

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        Geometry limit = new WKTReader().read("MULTIPOLYGON(((48 62, 48 63, 49 63, 49 62, 48 62)))");
        Filter filter = ff.intersects(ff.property(""), ff.literal(limit));

        assertEquals(filter, vl.getReadFilter());
        assertEquals(filter, vl.getWriteFilter());
    }

    /**
     * This test is very similar to testAreaLimited(), but the source resource is set to have the 900913 SRS. We expect
     * that the allowedarea is projected into the resource CRS.
     */
    @Ignore("revisit, not working originally")
    @Test
    public void testArea900913Vector() throws Exception {
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

        Geometry expectedLimit = new WKTReader()
                .read(
                        " MULTIPOLYGON (((5343335.558077131 8859142.800565697, 5343335.558077131 9100250.907059547, 5454655.048870404 9100250.907059547, 5454655.048870404 8859142.800565697, 5343335.558077131 8859142.800565697)))");

        IntersectExtractor ier = new IntersectExtractor();
        vl.getReadFilter().accept(ier, null);
        assertTrue(expectedLimit.equalsExact(ier.geom, .000000001));

        IntersectExtractor iew = new IntersectExtractor();
        vl.getWriteFilter().accept(iew, null);
        assertTrue(expectedLimit.equalsExact(iew.geom, .000000001));
    }

    @Ignore("revisit, not working originally")
    @Test
    public void testArea900913Raster() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");

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

        Authentication user = getUser("area", "area", "ROLE_AUTHENTICATED");
        login("area", "area", "ROLE_AUTHENTICATED");
        Request request = new Request();
        request.setService("wms");
        Dispatcher.REQUEST.set(request);

        // Check we have the geometry filter set
        CoverageAccessLimits accessLimits = (CoverageAccessLimits) accessManager.getAccessLimits(user, resource);

        Geometry expectedLimit = new WKTReader()
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
