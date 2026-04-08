/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.PostGISTestResource;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.junit.ClassRule;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.w3c.dom.Document;

public class WMSEnvParamsToViewPropagationTest extends WMSTestSupport {

    @ClassRule
    public static final PostGISTestResource postgis = new PostGISTestResource();

    @Override
    protected void onSetUp(SystemTestData data) throws Exception {
        super.setUpTestData(data);

        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("sqlviews");
        WorkspaceInfo ws = cat.getDefaultWorkspace();
        ds.setWorkspace(ws);
        ds.setEnabled(true);

        Map<String, Serializable> params = ds.getConnectionParameters();
        params.putAll(postgis.getConnectionParameters());
        cat.add(ds);

        SimpleFeatureSource fsp = getFeatureSource(SystemTestData.LAKES);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();

        tb.init(fsp.getSchema());
        tb.setName("lakes");
        SimpleFeatureType schema = tb.buildFeatureType();
        store.createSchema(schema);
        SimpleFeatureStore featureStore = (SimpleFeatureStore) store.getFeatureSource("lakes");
        featureStore.addFeatures(fsp.getFeatures());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);
        FeatureTypeInfo tft = cb.buildFeatureType(featureStore);
        cat.add(tft);

        JDBCDataStore jds = (JDBCDataStore) ds.getDataStore(null);
        VirtualTable vt = new VirtualTable(
                "lakes_view",
                "select \"NAME\", \"the_geom\", '%wms_bbox%' as bbox, '%wms_srs%' as srs, %wms_width% as width, %wms_height% as height, %wms_scale_denominator% as scale from \"lakes\" ");
        vt.addParameter(new VirtualTableParameter("wms_bbox", "1,1,1,1"));
        vt.addParameter(new VirtualTableParameter("wms_srs", "EPSG:404"));
        vt.addParameter(new VirtualTableParameter("wms_width", "20"));
        vt.addParameter(new VirtualTableParameter("wms_height", "21"));
        vt.addParameter(new VirtualTableParameter("wms_scale_denominator", "1000"));

        vt.addGeometryMetadatata("the_geom", MultiPolygon.class, 4326);
        jds.createVirtualTable(vt);

        ContentFeatureSource featureSource = jds.getFeatureSource(vt.getName());
        FeatureTypeInfo vft = cb.buildFeatureType(featureSource);
        cb.setupBounds(vft);
        cb.setupMetadata(vft, featureSource);
        vft.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);
        vft.setSRS("EPSG:4326");
        vft.setEnabled(true);
        cat.add(vft);

        LayerInfo layer = cat.getFactory().createLayer();
        layer.setResource(vft);
        layer.setName("lakes_view");
        layer.setEnabled(true);
        layer.setQueryable(true);
        cat.add(layer);

        Map<String, String> ns = new HashMap<>();
        ns.put("gml", "http://www.opengis.net/gml");
        ns.put("gs", "http://geoserver.org");
        SimpleNamespaceContext ctx = new SimpleNamespaceContext(ns);
        org.custommonkey.xmlunit.XMLUnit.setXpathNamespaceContext(ctx);
    }

    @Test
    public void testParamsSetToQuery() throws Exception {
        Document dom = getAsDOM(
                "wms?service=WMS&version=1.3.0&request=GetFeatureInfo&format=image/png&"
                        + "QUERY_LAYERS=gs:lakes_view&layers=gs:lakes_view&info_format=text/xml&SRS=EPSG:4326&"
                        + "feature_count=10&x=50&y=50&width=100&height=101&bbox=-0.0017300248146057129,0.0016710162162780762,-0.0011882185935974121,0.002212822437286377");

        assertXpathEvaluatesTo(
                "-0.001730,0.001671,-0.001188,0.002213", "//gml:featureMember/gs:lakes_view/gs:bbox", dom);

        assertXpathEvaluatesTo("urn:ogc:def:crs:EPSG::4326", "//gml:featureMember/gs:lakes_view/gs:srs", dom);

        assertXpathEvaluatesTo("100", "//gml:featureMember/gs:lakes_view/gs:width", dom);

        assertXpathEvaluatesTo("101", "//gml:featureMember/gs:lakes_view/gs:height", dom);

        assertXpathExists("//gml:featureMember/gs:lakes_view/gs:scale", dom);
    }
}
