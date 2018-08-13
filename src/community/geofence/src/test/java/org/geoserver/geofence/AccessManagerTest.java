/* (c) 2013-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AccessManagerTest extends GeofenceBaseTest {

    @Test
    public void testAdmin() {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "geoserver",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")
                                }));

        // check workspace access
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());

        // check layer access
        LayerInfo layer = catalog.getLayerByName(getLayerId(MockData.BASIC_POLYGONS));
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, layer);
        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
        assertNull(vl.getReadAttributes());
        assertNull(vl.getWriteAttributes());
    }

    @Test
    public void testCiteCannotWriteOnWorkspace() {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        configManager.getConfiguration().setGrantWriteToWorkspacesToAuthenticatedUsers(false);
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken(
                        "cite",
                        "cite",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_AUTHENTICATED")
                                }));

        // check workspace access
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertFalse(wl.isWritable());
    }

    @Test
    public void testCiteCanWriteOnWorkspace() {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        configManager.getConfiguration().setGrantWriteToWorkspacesToAuthenticatedUsers(true);
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken(
                        "cite",
                        "cite",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_AUTHENTICATED")
                                }));

        // check workspace access
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());
        configManager.getConfiguration().setGrantWriteToWorkspacesToAuthenticatedUsers(false);
    }

    @Test
    public void testAnonymousUser() {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        // check workspace access
        // WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        // WorkspaceAccessLimits wl = manager.getAccessLimits(null, citeWS);
        // assertFalse(wl.isReadable());
        // assertFalse(wl.isWritable());

        // check layer access
        LayerInfo layer = catalog.getLayerByName(getLayerId(MockData.BASIC_POLYGONS));
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(null, layer);
        assertEquals(Filter.EXCLUDE, vl.getReadFilter());
        assertEquals(Filter.EXCLUDE, vl.getWriteFilter());
        assertNull(vl.getReadAttributes());
        assertNull(vl.getWriteAttributes());
    }

    public void IGNOREtestCiteWorkspaceAccess() {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("cite", "cite");

        // check workspace access on cite
        WorkspaceInfo citeWS = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());

        // check workspace access on any other but not cite and sf (should fail)
        WorkspaceInfo cdfWS = catalog.getWorkspaceByName(MockData.CDF_PREFIX);
        wl = accessManager.getAccessLimits(user, cdfWS);
        assertFalse(wl.isReadable());
        assertFalse(wl.isWritable());

        // check workspace access on sf (should work, we can do at least a getmap)
        WorkspaceInfo sfWS = catalog.getWorkspaceByName(MockData.SF_PREFIX);
        wl = accessManager.getAccessLimits(user, sfWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());
    }

    @Test
    public void testCiteLayerAccess() {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("cite", "cite");

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
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("wmsuser", "wmsuser");

        // check layer in the sf workspace with a wfs request
        Request request = new Request();
        request.setService("WFS");
        request.setRequest("GetFeature");
        Dispatcher.REQUEST.set(request);

        LayerInfo generic = catalog.getLayerByName(getLayerId(MockData.GENERICENTITY));
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, generic);
        assertEquals(Filter.EXCLUDE, vl.getReadFilter());
        assertEquals(Filter.EXCLUDE, vl.getWriteFilter());

        // now fake a getmap request (using a service and request with a different case than the
        // geofenceService)
        request = new Request();
        request.setService("wms");
        Dispatcher.REQUEST.set(request);
        vl = (VectorAccessLimits) accessManager.getAccessLimits(user, generic);
        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
    }

    @Test
    public void testAreaLimited() throws Exception {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("area", "area");

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
    public void testArea900913() throws Exception {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("area", "area");

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
        Geometry limit =
                new WKTReader()
                        .read(
                                " MULTIPOLYGON (((5343335.558077131 8859142.800565697, 5343335.558077131 9100250.907059547, 5454655.048870404 9100250.907059547, 5454655.048870404 8859142.800565697, 5343335.558077131 8859142.800565697)))");
        Filter filter = ff.intersects(ff.property(""), ff.literal(limit));

        assertEquals(filter, vl.getReadFilter());
        assertEquals(filter, vl.getWriteFilter());
    }

    @Test
    public void testWmsGetMapRequestWithLayerGroupAndNormalLayerAndStyles() {
        if (!IS_GEOFENCE_AVAILABLE) {
            return;
        }

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        List<PublishedInfo> layers = new ArrayList<>();
        layers.add(catalog.getLayerByName("Buildings"));
        layers.add(catalog.getLayerByName("DividedRoutes"));
        List<StyleInfo> styles = new ArrayList<>();
        styles.add(catalog.getLayerByName("Buildings").getDefaultStyle());
        styles.add(catalog.getLayerByName("DividedRoutes").getDefaultStyle());
        LayerGroupInfoImpl layerGroup = new LayerGroupInfoImpl();
        layerGroup.setName("layer_group");
        layerGroup.setLayers(layers);
        layerGroup.setStyles(styles);
        catalog.add(layerGroup);
        Map kvp = new HashMap<>();
        kvp.put("LAYERS", "layer_group,Bridges");
        kvp.put("layers", "layer_group,Bridges");
        kvp.put("STYLES", ",lines");
        Request gsRequest = new Request();
        gsRequest.setKvp(kvp);
        gsRequest.setRawKvp(kvp);
        String service = "WMS";
        String requestName = "GetMap";
        Authentication user =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "geoserver",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")
                                }));
        SecurityContextHolder.getContext().setAuthentication(user);
        List<MapLayerInfo> mapLayersInfos = new ArrayList<>();
        mapLayersInfos.add(new MapLayerInfo(catalog.getLayerByName("Buildings")));
        mapLayersInfos.add(new MapLayerInfo(catalog.getLayerByName("DividedRoutes")));
        mapLayersInfos.add(new MapLayerInfo(catalog.getLayerByName("Bridges")));
        GetMapRequest getMap = new GetMapRequest();
        getMap.setLayers(mapLayersInfos);
        accessManager.overrideGetMapRequest(gsRequest, service, requestName, user, getMap);
    }
}
