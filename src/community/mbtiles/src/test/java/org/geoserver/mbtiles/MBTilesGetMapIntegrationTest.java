/*
 * (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mbtiles;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.geoserver.catalog.*;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.api.data.*;
import org.geotools.data.*;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.test.ImageAssert;
import org.geotools.mbtiles.MBTilesDataStoreFactory;
import org.junit.Test;

public class MBTilesGetMapIntegrationTest extends WMSTestSupport {

    String bbox = "4254790.681588205,4619242.456803064,4701182.96838953,4977579.240638782";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("heathmap", "heathmap_style.sld", getClass(), catalog);
        testData.addStyle("barnes_surface", "barnes_surface_style.sld", getClass(), catalog);
        WorkspaceInfo ws = catalog.getWorkspaceByName("cite");
        DataStoreInfo store =
                addMBStore(catalog, ws, "many_points", "manypoints_test.mbtiles", getClass());
        addMBVectorLayer(catalog, ws, store, "manypoints_test");
    }

    @Test
    public void testHeathMapProcess() throws Exception {
        // Test a heathmap process on features coming from a mbtiles vector store
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName("heathmap");
        LayerInfo info = catalog.getLayerByName("manypoints_test");
        info.setDefaultStyle(style);
        catalog.save(info);
        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&layers=cite:manypoints_test"
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=447"
                                + "&height=330"
                                + "&srs=EPSG:3857",
                        "image/png");

        URL expectedResponse = getClass().getResource("heathmap_many_points.png");
        BufferedImage expectedImage = ImageIO.read(expectedResponse);
        ImageAssert.assertEquals(image, expectedImage, 200);
    }

    @Test
    public void testBarnesSurface() throws Exception {
        // Test Barnes surface process on features coming from a mbtiles vector store
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName("barnes_surface");
        LayerInfo info = catalog.getLayerByName("manypoints_test");
        info.setDefaultStyle(style);
        catalog.save(info);

        BufferedImage image =
                getAsImage(
                        "wms?bbox="
                                + bbox
                                + "&layers=cite:manypoints_test"
                                + "&Format=image/png"
                                + "&request=GetMap"
                                + "&width=447"
                                + "&height=330"
                                + "&srs=EPSG:3857",
                        "image/png");
        URL expectedResponse = getClass().getResource("barnes_many_points.png");
        BufferedImage expectedImage = ImageIO.read(expectedResponse);
        ImageAssert.assertEquals(image, expectedImage, 500);
    }

    private DataStoreInfo addMBStore(
            Catalog catalog, WorkspaceInfo ws, String storeName, String fileName, Class<?> scope)
            throws URISyntaxException {
        DataStoreInfo store = catalog.getDataStoreByName(storeName);
        if (store == null) {

            store = catalog.getFactory().createDataStore();
            store.setName(storeName);
            store.setWorkspace(ws);
            store.setEnabled(true);
            URI uri = scope.getResource(fileName).toURI();
            NamespaceInfo namespace = catalog.getNamespaceByPrefix(ws.getName());
            store.getConnectionParameters().put("database", uri.toString());
            store.getConnectionParameters()
                    .put(PropertyDataStoreFactory.NAMESPACE.key, namespace.getURI());
            store.getConnectionParameters().put("dbtype", "mbtiles");
            store.setType(new MBTilesDataStoreFactory().getDisplayName());
            catalog.add(store);
        }
        return catalog.getDataStoreByName(ws.getName(), storeName);
    }

    private void addMBVectorLayer(
            Catalog catalog, WorkspaceInfo ws, DataStoreInfo store, String typeName)
            throws IOException {
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace(ws);
        builder.setStore(store);
        DataAccess dataAccess = store.getDataStore(null);
        Map<String, FeatureTypeInfo> featureTypesByNativeName = new HashMap<>();
        FeatureSource fs = ((DataStore) dataAccess).getFeatureSource(typeName);
        FeatureTypeInfo ftinfo = featureTypesByNativeName.get(typeName);
        if (ftinfo == null) {
            ftinfo = builder.buildFeatureType(fs);
            builder.lookupSRS(ftinfo, true);
            builder.setupBounds(ftinfo, fs);
        }

        ReferencedEnvelope bounds = fs.getBounds();
        ftinfo.setNativeBoundingBox(bounds);

        if (ftinfo.getId() == null) {
            catalog.validate(ftinfo, true).throwIfInvalid();
            catalog.add(ftinfo);
        }

        LayerInfo layer = builder.buildLayer(ftinfo);

        boolean valid = true;
        try {
            if (!catalog.validate(layer, true).isValid()) {
                valid = false;
            }
        } catch (Exception e) {
            valid = false;
        }

        layer.setEnabled(valid);
        catalog.add(layer);
    }
}
