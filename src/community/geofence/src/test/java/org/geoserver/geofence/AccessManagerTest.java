/* (c) 2013-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import java.util.*;
import org.geoserver.catalog.*;
import org.geoserver.catalog.impl.*;
import org.geoserver.data.test.MockData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Test;
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

    /** Override to have the code access the raw catalog */
    protected Catalog getCatalog() {
        return (Catalog) applicationContext.getBean("rawCatalog");
    }

    public void testAdmin() {
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "geoserver",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")
                                }));

        // check workspace access
        WorkspaceInfo citeWS = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());

        // check layer access
        LayerInfo layer = getCatalog().getLayerByName(getLayerId(MockData.BASIC_POLYGONS));
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, layer);
        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
        assertNull(vl.getReadAttributes());
        assertNull(vl.getWriteAttributes());
    }

    public void testCiteCannotWriteOnWorkspace() {
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
        WorkspaceInfo citeWS = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertFalse(wl.isWritable());
    }

    public void testCiteCanWriteOnWorkspace() {
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
        WorkspaceInfo citeWS = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());
        configManager.getConfiguration().setGrantWriteToWorkspacesToAuthenticatedUsers(false);
    }

    @Test
    public void testAnonymousUser() {
        // check workspace access
        // WorkspaceInfo citeWS = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
        // WorkspaceAccessLimits wl = manager.getAccessLimits(null, citeWS);
        // assertFalse(wl.isReadable());
        // assertFalse(wl.isWritable());

        // check layer access
        LayerInfo layer = getCatalog().getLayerByName(getLayerId(MockData.BASIC_POLYGONS));
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(null, layer);
        assertEquals(Filter.EXCLUDE, vl.getReadFilter());
        assertEquals(Filter.EXCLUDE, vl.getWriteFilter());
        assertNull(vl.getReadAttributes());
        assertNull(vl.getWriteAttributes());
    }

    public void IGNOREtestCiteWorkspaceAccess() {
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("cite", "cite");

        // check workspace access on cite
        WorkspaceInfo citeWS = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());

        // check workspace access on any other but not cite and sf (should fail)
        WorkspaceInfo cdfWS = getCatalog().getWorkspaceByName(MockData.CDF_PREFIX);
        wl = accessManager.getAccessLimits(user, cdfWS);
        assertFalse(wl.isReadable());
        assertFalse(wl.isWritable());

        // check workspace access on sf (should work, we can do at least a getmap)
        WorkspaceInfo sfWS = getCatalog().getWorkspaceByName(MockData.SF_PREFIX);
        wl = accessManager.getAccessLimits(user, sfWS);
        assertTrue(wl.isReadable());
        assertTrue(wl.isWritable());
    }

    public void testCiteLayerAccess() {
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("cite", "cite");

        // check layer in the cite workspace
        LayerInfo bpolygons = getCatalog().getLayerByName(getLayerId(MockData.BASIC_POLYGONS));
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

        LayerInfo generic = getCatalog().getLayerByName(getLayerId(MockData.GENERICENTITY));
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

    public void testWmsLimited() {
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("wmsuser", "wmsuser");

        // check layer in the sf workspace with a wfs request
        Request request = new Request();
        request.setService("WFS");
        request.setRequest("GetFeature");
        Dispatcher.REQUEST.set(request);

        LayerInfo generic = getCatalog().getLayerByName(getLayerId(MockData.GENERICENTITY));
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

    public void testAreaLimited() throws Exception {
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("area", "area");

        // check we have the geometry filter set
        LayerInfo generic = getCatalog().getLayerByName(getLayerId(MockData.GENERICENTITY));
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
    public void testArea900913() throws Exception {
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken("area", "area");

        LayerInfo generic = getCatalog().getLayerByName(getLayerId(MockData.GENERICENTITY));

        // Create a layer using as much as info from the Mock instance, making sure we're declaring
        // the 900913 SRS.
        WorkspaceInfoImpl ws = new WorkspaceInfoImpl();
        ws.setName(generic.getResource().getStore().getWorkspace().getName());

        StoreInfo store = new DataStoreInfoImpl(getCatalog());
        store.setWorkspace(ws);

        FeatureTypeInfoImpl resource = new FeatureTypeInfoImpl(getCatalog());
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
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        List<PublishedInfo> layers = new ArrayList<>();
        layers.add(getCatalog().getLayerByName("Buildings"));
        layers.add(getCatalog().getLayerByName("DividedRoutes"));
        List<StyleInfo> styles = new ArrayList<>();
        styles.add(getCatalog().getLayerByName("Buildings").getDefaultStyle());
        styles.add(getCatalog().getLayerByName("DividedRoutes").getDefaultStyle());
        LayerGroupInfoImpl layerGroup = new LayerGroupInfoImpl();
        layerGroup.setName("layer_group");
        layerGroup.setLayers(layers);
        layerGroup.setStyles(styles);
        getCatalog().add(layerGroup);
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
        mapLayersInfos.add(new MapLayerInfo(getCatalog().getLayerByName("Buildings")));
        mapLayersInfos.add(new MapLayerInfo(getCatalog().getLayerByName("DividedRoutes")));
        mapLayersInfos.add(new MapLayerInfo(getCatalog().getLayerByName("Bridges")));
        GetMapRequest getMap = new GetMapRequest();
        getMap.setLayers(mapLayersInfos);
        accessManager.overrideGetMapRequest(gsRequest, service, requestName, user, getMap);
    }
}
