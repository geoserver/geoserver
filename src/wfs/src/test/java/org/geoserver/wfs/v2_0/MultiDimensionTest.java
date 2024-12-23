/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;

import java.net.URL;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.api.feature.type.Name;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class MultiDimensionTest extends WFS20TestSupport {
    private XpathEngine xpath;

    @Before
    public void setupXPathEngine() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        DataStoreInfo storeInfo = createShapefileDataStore(getCatalog(), "tasmania_roads", "tasmania_roads.shp");

        createShapeLayer(getCatalog(), storeInfo);
    }

    @Test
    public void testGetZDimension() throws Exception {
        Document dom = getAsDOM("wfs?request=GetFeature&typenames=gs:tasmania_roads&version=2.0.0&service=wfs");
        print(dom);
        assertEquals("tasmania_roads.1", xpath.evaluate("//gs:tasmania_roads/@gml:id", dom));
        assertEquals("3", xpath.evaluate("//gs:tasmania_roads/gs:the_geom/gml:MultiCurve/@srsDimension", dom));
    }

    private static DataStoreInfo createShapefileDataStore(Catalog catalog, String name, String file) {
        // get the file
        URL url = MultiDimensionTest.class.getResource(file);
        assertThat(url, notNullValue());
        // build the data store
        CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);
        DataStoreInfo storeInfo = catalogBuilder.buildDataStore(name);
        storeInfo.setType("Shapefile");
        storeInfo.getConnectionParameters().put(ShapefileDataStoreFactory.URLP.key, url);
        catalog.add(storeInfo);
        // return the wfs data store we just build
        storeInfo = catalog.getStoreByName(name, DataStoreInfo.class);
        assertThat(storeInfo, notNullValue());
        return storeInfo;
    }

    /**
     * Helper method that creates a layer in GeoServer catalog from a WFS remote store, the provided layer name should
     * match an entry on the remote WFS server.
     */
    private static LayerInfo createShapeLayer(Catalog catalog, DataStoreInfo storeInfo) throws Exception {
        // let's create the feature type based on the remote layer capabilities
        // description
        CatalogBuilder catalogBuilder = new CatalogBuilder(catalog);
        catalogBuilder.setStore(storeInfo);
        // the following call will trigger a describe feature type call to the
        // remote server

        Name typeName = storeInfo.getDataStore(null).getNames().get(0);
        FeatureTypeInfo featureTypeInfo = catalogBuilder.buildFeatureType(typeName);
        catalog.add(featureTypeInfo);
        // create the layer info based on the feature type info we just created
        LayerInfo layerInfo = catalogBuilder.buildLayer(featureTypeInfo);
        catalog.add(layerInfo);
        // return the layer info we just created
        layerInfo = catalog.getLayerByName(typeName.getLocalPart());
        assertThat(layerInfo, notNullValue());
        return layerInfo;
    }
}
