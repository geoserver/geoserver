/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.PostGISTestResource;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.junit.ClassRule;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class SQLViewTest extends WFSTestSupport {

    static final String tableTypeName = "gs:pgeo";
    static final String viewTypeName = "gs:pgeo_view";

    @ClassRule
    public static final PostGISTestResource postgis = new PostGISTestResource();

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        // run all the tests against a store that can do sql views
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("sqlviews");
        WorkspaceInfo ws = cat.getDefaultWorkspace();
        ds.setWorkspace(ws);
        ds.setEnabled(true);

        Map<String, Serializable> params = ds.getConnectionParameters();
        params.putAll(postgis.getConnectionParameters());
        cat.add(ds);
        SimpleFeatureSource fsp = getFeatureSource(SystemTestData.PRIMITIVEGEOFEATURE);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();

        tb.init(fsp.getSchema());
        tb.remove("surfaceProperty"); // the store cannot create multi-geom tables it seems
        tb.remove("curveProperty"); // the store cannot create multi-geom tables it seems
        tb.remove("uriProperty"); // this would render the store read only
        tb.setName("pgeo");
        SimpleFeatureType schema = tb.buildFeatureType();
        store.createSchema(schema);
        SimpleFeatureStore featureStore = (SimpleFeatureStore) store.getFeatureSource("pgeo");
        featureStore.addFeatures(fsp.getFeatures());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);
        FeatureTypeInfo tft = cb.buildFeatureType(featureStore);
        cat.add(tft);

        // create the sql view
        JDBCDataStore jds = (JDBCDataStore) ds.getDataStore(null);
        VirtualTable vt = new VirtualTable(
                "pgeo_view",
                "select \"name\", \"pointProperty\" from \"pgeo\" where \"booleanProperty\" = %bool% and \"name\" = '%name%'");
        vt.addParameter(new VirtualTableParameter("bool", "true"));
        vt.addParameter(new VirtualTableParameter("name", "name-f001"));
        vt.addGeometryMetadatata("pointProperty", Point.class, 4326);
        jds.createVirtualTable(vt);

        FeatureTypeInfo vft = cb.buildFeatureType(jds.getFeatureSource(vt.getName()));
        vft.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);
        cat.add(vft);
    }

    /** Checks the setup did the expected job */
    @Test
    public void testStoreSetup() throws Exception {
        FeatureTypeInfo tableTypeInfo = getCatalog().getFeatureTypeByName(tableTypeName);
        assertNotNull(tableTypeInfo);
        assertEquals(5, tableTypeInfo.getFeatureSource(null, null).getCount(Query.ALL));

        FeatureTypeInfo viewTypeInfo = getCatalog().getFeatureTypeByName(viewTypeName);
        assertNotNull(viewTypeInfo);
        assertEquals(1, viewTypeInfo.getFeatureSource(null, null).getCount(Query.ALL));
    }

    @Test
    public void testPostWithXMLViewParams_200() throws Exception {
        String xmlViewParams =
                StringEscapeUtils.escapeXml10("<VP><PS><P n=\"bool\">true</P><P n=\"name\">name-f003</P></PS></VP>");
        String xml = "<wfs:GetFeature service=\"WFS\" version=\"2.0.0\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" "
                + "viewParams=\""
                + xmlViewParams
                + "\"> "
                + "<wfs:Query typeNames=\""
                + viewTypeName
                + "\">"
                + "</wfs:Query></wfs:GetFeature>";

        Document doc = postAsDOM("wfs?viewParamsFormat=XML", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList features = doc.getElementsByTagName("gs:pgeo_view");
        assertEquals(1, features.getLength());
        assertEquals("gml:name", features.item(0).getFirstChild().getNodeName());
        assertEquals("name-f003", features.item(0).getFirstChild().getTextContent());
    }

    @Test
    public void testPostWithViewParams_200() throws Exception {
        String xml = "<wfs:GetFeature service=\"WFS\" version=\"2.0.0\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs/2.0\" "
                + "viewParams=\"bool:true;name:name-f003\"> "
                + "<wfs:Query typeNames=\""
                + viewTypeName
                + "\">"
                + "</wfs:Query></wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList features = doc.getElementsByTagName("gs:pgeo_view");
        assertEquals(1, features.getLength());
        assertEquals("gml:name", features.item(0).getFirstChild().getNodeName());
        assertEquals("name-f003", features.item(0).getFirstChild().getTextContent());
    }
}
