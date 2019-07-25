/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class CapabilitiesRootLayerTest extends WMSTestSupport {

    public CapabilitiesRootLayerTest() {
        super();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Catalog catalog = getCatalog();
        DataStoreInfo info = catalog.getDataStoreByName(MockData.SF_PREFIX);
        info.setEnabled(false);
        catalog.save(info);

        GeoServerInfo global = getGeoServer().getGlobal();
        global.getSettings().setProxyBaseUrl("src/test/resources/geoserver");
        getGeoServer().save(global);

        // add a workspace qualified style
        WorkspaceInfo ws = catalog.getWorkspaceByName(MockData.CITE_PREFIX);
        testData.addStyle(ws, "Lakes", "Lakes.sld", SystemTestData.class, catalog);
        StyleInfo lakesStyle = catalog.getStyleByName(ws, "Lakes");
        LayerInfo lakesLayer = catalog.getLayerByName(MockData.LAKES.getLocalPart());
        lakesLayer.setDefaultStyle(lakesStyle);
        catalog.save(lakesLayer);

        for (LayerGroupInfo layerGroupInfo : catalog.getLayerGroups()) {
            catalog.remove(layerGroupInfo);
        }
    }

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
    }

    @Test
    public void testRootLayerNotRemoved() throws Exception {
        // make layers non advertised
        WMSInfo info = getWMS().getServiceInfo();
        info.getMetadata().put(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY, true);
        getGeoServer().save(info);
        Catalog catalog = getCatalog();
        LayerGroupInfo group = null;
        try {
            group = createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.NAMED, null);
            Document dom = getAsDOM("lakes_and_places/wms?request=GetCapabilities&version=1.3.0");
            // print(dom);

            assertXpathEvaluatesTo(
                    "", "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Name", dom);
            assertXpathEvaluatesTo(
                    "lakes_and_places",
                    "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Layer/wms:Name",
                    dom);
        } finally {
            if (group != null) {
                catalog.remove(group);
            }
            info.getMetadata().remove(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY);
        }
    }

    @Test
    public void testRootLayerRemovedGroupConfig() throws Exception {
        // make layers non advertised
        Catalog catalog = getCatalog();
        LayerGroupInfo group = null;
        try {
            group = createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.NAMED, null);
            group.getMetadata().put(PublishedInfo.ROOT_IN_CAPABILITIES, false);
            Document dom = getAsDOM("lakes_and_places/wms?request=GetCapabilities&version=1.3.0");
            // print(dom);

            assertXpathEvaluatesTo(
                    "lakes_and_places",
                    "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Name",
                    dom);
        } finally {
            if (group != null) {
                catalog.remove(group);
            }
        }
    }

    @Test
    public void testRootLayerRemovedLayerConfig() throws Exception {
        // make layers non advertised
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(MockData.LAKES.getLocalPart());

        try {
            layer.getMetadata().put(PublishedInfo.ROOT_IN_CAPABILITIES, false);
            catalog.save(layer);
            Document dom = getAsDOM("cite/Lakes/wms?request=GetCapabilities&version=1.3.0");
            // print(dom);

            assertXpathEvaluatesTo(
                    "Lakes", "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Name", dom);
        } finally {
            layer.getMetadata().remove(PublishedInfo.ROOT_IN_CAPABILITIES);
            catalog.save(layer);
        }
    }

    @Test
    public void testRootLayerRemovedRequestParam() throws Exception {
        // make layers non advertised
        Catalog catalog = getCatalog();
        LayerGroupInfo group = null;
        try {
            group = createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.NAMED, null);
            Document dom =
                    getAsDOM(
                            "lakes_and_places/wms?request=GetCapabilities&version=1.3.0&rootLayer=false");
            // print(dom);

            assertXpathEvaluatesTo(
                    "lakes_and_places",
                    "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Name",
                    dom);
        } finally {
            if (group != null) {
                catalog.remove(group);
            }
        }
    }

    @Test
    public void testRootLayerRemovedRequestParamHasGreaterPriority() throws Exception {
        // make layers non advertised
        WMSInfo info = getWMS().getServiceInfo();
        info.getMetadata().put(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY, false);
        Catalog catalog = getCatalog();
        LayerGroupInfo group = null;
        try {
            group = createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.NAMED, null);
            group.getMetadata().put(PublishedInfo.ROOT_IN_CAPABILITIES, true);
            Document dom =
                    getAsDOM(
                            "lakes_and_places/wms?request=GetCapabilities&version=1.3.0&rootLayer=true");
            // print(dom);

            assertXpathEvaluatesTo(
                    "", "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Name", dom);
            assertXpathEvaluatesTo(
                    "lakes_and_places",
                    "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Layer/wms:Name",
                    dom);
        } finally {
            if (group != null) {
                catalog.remove(group);
            }
            info.getMetadata().remove(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY);
        }
    }

    @Test
    public void testRootLayerRemovedGroupConfigHasPriorityOnService() throws Exception {
        // make layers non advertised
        WMSInfo info = getWMS().getServiceInfo();
        info.getMetadata().put(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY, false);
        Catalog catalog = getCatalog();
        LayerGroupInfo group = null;
        try {
            group = createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.NAMED, null);
            group.getMetadata().put(PublishedInfo.ROOT_IN_CAPABILITIES, true);
            Document dom = getAsDOM("lakes_and_places/wms?request=GetCapabilities&version=1.3.0");
            // print(dom);

            assertXpathEvaluatesTo(
                    "", "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Name", dom);
            assertXpathEvaluatesTo(
                    "lakes_and_places",
                    "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Layer/wms:Name",
                    dom);
        } finally {
            if (group != null) {
                catalog.remove(group);
            }
            info.getMetadata().remove(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY);
        }
    }

    @Test
    public void testRootLayerRemovedWMSService() throws Exception {
        // make layers non advertised
        WMSInfo info = getWMS().getServiceInfo();
        info.getMetadata().put(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY, false);
        getGeoServer().save(info);
        Catalog catalog = getCatalog();
        LayerGroupInfo group = null;
        try {
            group = createLakesPlacesLayerGroup(catalog, LayerGroupInfo.Mode.NAMED, null);
            Document dom =
                    getAsDOM(
                            "lakes_and_places/wms?service=WMS&request=getCapabilities&version=1.3.0");
            // print(dom);

            assertXpathEvaluatesTo(
                    "lakes_and_places",
                    "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Name",
                    dom);
        } finally {
            if (group != null) {
                catalog.remove(group);
            }
            info.getMetadata().remove(WMS.ROOT_LAYER_IN_CAPABILITIES_KEY);
        }
    }

    @Test
    public void testRootLayerRemovedLayerIfSingleConfig() throws Exception {
        Catalog catalog = getCatalog();
        LayerInfo layer = catalog.getLayerByName(MockData.LAKES.getLocalPart());
        for (LayerInfo layerInfo : catalog.getLayers()) {
            if (!layerInfo.getName().equals(MockData.LAKES.getLocalPart())) {
                layerInfo.setAdvertised(false);
                catalog.save(layerInfo);
            }
        }
        try {
            layer.getMetadata().put(PublishedInfo.ROOT_IN_CAPABILITIES, false);
            catalog.save(layer);
            Document dom = getAsDOM("cite/wms?request=GetCapabilities&version=1.3.0");
            // print(dom);

            assertXpathEvaluatesTo(
                    "Lakes", "/wms:WMS_Capabilities/wms:Capability/wms:Layer/wms:Name", dom);
        } finally {
            layer.getMetadata().remove(PublishedInfo.ROOT_IN_CAPABILITIES);
            catalog.save(layer);
            for (LayerInfo layerInfo : catalog.getLayers()) {
                if (!layerInfo.getName().equals(MockData.LAKES.getLocalPart())) {
                    layerInfo.setAdvertised(true);
                    catalog.save(layerInfo);
                }
            }
        }
    }
}
