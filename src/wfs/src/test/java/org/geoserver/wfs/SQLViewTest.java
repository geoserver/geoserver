/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Map;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.junit.Test;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class SQLViewTest extends WFSTestSupport {

    static final String tableTypeName = "gs:pgeo";
    static final String viewTypeName = "gs:pgeo_view";

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        // run all the tests against a store that can do sql views
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("sqlviews");
        WorkspaceInfo ws = cat.getDefaultWorkspace();
        ds.setWorkspace(ws);
        ds.setEnabled(true);

        Map params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        File dbFile =
                new File(getTestData().getDataDirectoryRoot().getAbsolutePath(), "data/h2test");
        params.put("database", dbFile.getAbsolutePath());
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
        VirtualTable vt =
                new VirtualTable(
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
    public void testViewParamsGet() throws Exception {
        Document dom =
                getAsDOM(
                        "wfs?service=WFS&request=GetFeature&typename="
                                + viewTypeName
                                + "&version=1.1&viewparams=bool:true;name:name-f003");
        // print(dom);

        assertXpathEvaluatesTo("name-f003", "//gs:pgeo_view/gml:name", dom);
        assertXpathEvaluatesTo("1", "count(//gs:pgeo_view)", dom);
    }

    @Test
    public void testViewParamsJsonGet() throws Exception {
        JSON json =
                getAsJSON(
                        "wfs?service=WFS&request=GetFeature&typename="
                                + viewTypeName
                                + "&version=1.1&viewparams=bool:true;name:name-f003&outputFormat=application/json");
        // print(json);

        assertEquals(1, ((JSONObject) json).getInt("totalFeatures"));
    }

    @Test
    public void testPostWithViewParams_v100() throws Exception {
        String xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.0.0\" "
                        + "viewParams=\"bool:true;name:name-f003\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" > "
                        + "<wfs:Query typeName=\""
                        + viewTypeName
                        + "\"> "
                        + "</wfs:Query></wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureMembers = doc.getElementsByTagName("gml:featureMember");
        assertFalse(featureMembers.getLength() == 0);
        assertXpathEvaluatesTo("name-f003", "//gs:pgeo_view/gs:name", doc);
        assertXpathEvaluatesTo("1", "count(//gs:pgeo_view)", doc);
    }

    @Test
    public void testPostWithViewParams_110() throws Exception {

        String xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" "
                        + "viewParams=\"bool:true;name:name-f003\" "
                        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" > "
                        + "<wfs:Query typeName=\""
                        + viewTypeName
                        + "\"> "
                        + "</wfs:Query></wfs:GetFeature>";

        Document doc = postAsDOM("wfs", xml);
        assertEquals("wfs:FeatureCollection", doc.getDocumentElement().getNodeName());

        NodeList featureCollection = doc.getElementsByTagName("wfs:FeatureCollection");
        assertFalse(featureCollection.getLength() == 0);
        assertXpathEvaluatesTo("name-f003", "//gs:pgeo_view/gml:name", doc);
        assertXpathEvaluatesTo("1", "count(//gs:pgeo_view)", doc);
    }

    @Test
    public void testPostWithViewParams_200() throws Exception {
        String xml =
                "<wfs:GetFeature service=\"WFS\" version=\"2.0.0\" "
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
        assertEquals(features.item(0).getFirstChild().getNodeName(), "gml:name");
        assertEquals(features.item(0).getFirstChild().getTextContent(), "name-f003");
    }
}
