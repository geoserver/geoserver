/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.junit.BeforeClass;
import org.junit.Test;

public class MapMLWMSProxyTest extends MapMLBaseProxyTest {

    @BeforeClass
    public static void beforeClass() {
        initMockService("/mockgeoserver", "/wms", "REQUEST=GetCapabilities&VERSION=1.3.0&SERVICE=WMS", "wmscaps.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();

        WMSStoreInfo wmsStore = catalog.getFactory().createWebMapServer();
        wmsStore.setName("wmsStore");
        wmsStore.setWorkspace(catalog.getDefaultWorkspace());
        wmsStore.setCapabilitiesURL(getCapabilitiesURL());
        wmsStore.setEnabled(true);
        catalog.add(wmsStore);

        // Create WMSLayerInfo using the Catalog factory
        WMSLayerInfo wmsLayer = catalog.getFactory().createWMSLayer();
        wmsLayer.setName("cascadedLayer");
        wmsLayer.setNativeName("topp:states");
        wmsLayer.setStore(wmsStore);
        wmsLayer.setAdvertised(true);
        wmsLayer.setEnabled(true);

        // Add the layer to the catalog
        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(wmsLayer);
        layer.setDefaultStyle(catalog.getStyleByName("default"));
        catalog.add(wmsLayer);
        catalog.add(layer);
    }

    @Test
    public void testMapMLRemoteFlag() throws Exception {
        Catalog cat = getCatalog();
        // Verify the layer was added
        LayerInfo layerInfo = cat.getLayerByName("cascadedLayer");
        ResourceInfo layerMeta = layerInfo.getResource();
        layerMeta.getMetadata().put(MapMLConstants.MAPML_USE_REMOTE, false);
        cat.save(layerMeta);

        // get the mapml doc for the layer
        String path = BASE_REQUEST;

        // Verify that Remote set to false is not cascading
        checkCascading(path, false, MapMLConstants.REL_IMAGE, true);

        // Now switching to use Remote URL
        layerMeta.getMetadata().put(MapMLConstants.MAPML_USE_REMOTE, true);
        cat.save(layerMeta);

        // verify that is cascading to the remote URL
        checkCascading(path, true, MapMLConstants.REL_IMAGE, true);
    }

    @Test
    public void testMapMLUnsupportedCRSNotCascading() throws Exception {
        Catalog cat = getCatalog();
        // Verify the layer was added
        LayerInfo layerInfo = cat.getLayerByName("cascadedLayer");
        ResourceInfo layerMeta = layerInfo.getResource();
        layerMeta.getMetadata().put(MapMLConstants.MAPML_USE_REMOTE, true);
        cat.save(layerMeta);

        // get the mapml doc for the layer
        String path = BASE_REQUEST.replace("EPSG:4326", "EPSG:3857");

        // Verify that asking unsupported CRS in the remote layer is not cascading
        checkCascading(path, false, MapMLConstants.REL_IMAGE, true);
    }

    @Test
    public void testMapMLVendorOptionsNotCascading() throws Exception {
        Catalog cat = getCatalog();
        // Verify the layer was added
        LayerInfo layerInfo = cat.getLayerByName("cascadedLayer");
        ResourceInfo layerMeta = layerInfo.getResource();
        layerMeta.getMetadata().put(MapMLConstants.MAPML_USE_REMOTE, true);
        cat.save(layerMeta);

        // Setting up a WMS vendor options.
        String path = BASE_REQUEST
                + "&interpolations=bilinear"
                + "&clip=srid=3857;POLYGON ((-1615028.3514525702 7475148.401208023, 3844409.956787858 7475148.401208023, 3844409.956787858 3815954.983140064, -1615028.3514525702 3815954.983140064, -1615028.3514525702 7475148.401208023))";

        // Verify vendor option is not cascading
        checkCascading(path, false, MapMLConstants.REL_IMAGE, true);
    }

    @Test
    public void testMapMLCQLFilterNotCascading() throws Exception {
        Catalog cat = getCatalog();
        // Verify the layer was added
        LayerInfo layerInfo = cat.getLayerByName("cascadedLayer");
        ResourceInfo layerMeta = layerInfo.getResource();
        layerMeta.getMetadata().put(MapMLConstants.MAPML_USE_REMOTE, true);
        cat.save(layerMeta);

        // Setting up a WMS vendor options.
        String path = BASE_REQUEST + "&cql_filter=STATE_ABBR='MO'";

        // Verify vendor option is not cascading
        checkCascading(path, false, MapMLConstants.REL_IMAGE, true);
    }
}
