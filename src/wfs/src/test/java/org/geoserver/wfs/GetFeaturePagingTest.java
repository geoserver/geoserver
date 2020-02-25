/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Map;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.w3c.dom.Document;

public class GetFeaturePagingTest extends WFSTestSupport {

    @Override
    protected void setUpInternal(SystemTestData testData) throws Exception {
        // run all the tests against a store that can do native paging (h2) and one that
        // can't (property)
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("foo");
        ds.setWorkspace(cat.getDefaultWorkspace());

        Map params = ds.getConnectionParameters();
        params.put("dbtype", "h2");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath());
        ds.setEnabled(true);
        cat.add(ds);

        FeatureSource fs1 = getFeatureSource(SystemTestData.FIFTEEN);
        FeatureSource fs2 = getFeatureSource(SystemTestData.SEVEN);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();

        tb.init((SimpleFeatureType) fs1.getSchema());
        tb.remove("boundedBy");
        store.createSchema(tb.buildFeatureType());

        tb.init((SimpleFeatureType) fs2.getSchema());
        tb.remove("boundedBy");
        store.createSchema(tb.buildFeatureType());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        FeatureStore fs = (FeatureStore) store.getFeatureSource("Fifteen");
        fs.addFeatures(fs1.getFeatures());
        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        cat.add(ft);

        fs = (FeatureStore) store.getFeatureSource("Seven");
        fs.addFeatures(fs2.getFeatures());
        ft = cb.buildFeatureType(fs);
        cat.add(ft);
    }

    @Test
    public void testSingleType() throws Exception {
        doTestSingleType("gs:Fifteen");
        doTestSingleType("cdf:Fifteen");
    }

    void doTestSingleType(String typeName) throws Exception {

        Document doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.0.0&service=wfs&"
                                + "typename="
                                + typeName
                                + "&startIndex=10");
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + typeName + ")", doc);

        doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.0.0&service=wfs&"
                                + "typename="
                                + typeName
                                + "&startIndex=16");
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + typeName + ")", doc);

        doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.0.0&service=wfs&"
                                + "typename="
                                + typeName
                                + "&startIndex=0");
        XMLAssert.assertXpathEvaluatesTo("15", "count(//" + typeName + ")", doc);

        doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.0.0&service=wfs&"
                                + "typename="
                                + typeName
                                + "&startIndex=1&maxFeatures=1");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + ")", doc);

        doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.0.0&service=wfs&"
                                + "typename="
                                + typeName
                                + "&startIndex=16&maxFeatures=1");
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + typeName + ")", doc);
    }

    @Test
    public void testStartIndexSimplePOST() throws Exception {
        doTestStartIndexSimplePOST("gs:Fifteen");
        doTestStartIndexSimplePOST("cdf:Fifteen");
    }

    void doTestStartIndexSimplePOST(String typeName) throws Exception {

        Document doc = postAsDOM("wfs", startIndexSimpleXML(typeName, 10, -1));
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + typeName + ")", doc);

        doc = postAsDOM("wfs", startIndexSimpleXML(typeName, 16, -1));
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + typeName + ")", doc);

        doc = postAsDOM("wfs", startIndexSimpleXML(typeName, 0, -1));
        XMLAssert.assertXpathEvaluatesTo("15", "count(//" + typeName + ")", doc);

        doc = postAsDOM("wfs", startIndexSimpleXML(typeName, 1, 1));
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + ")", doc);

        doc = postAsDOM("wfs", startIndexSimpleXML(typeName, 16, 1));
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + typeName + ")", doc);
    }

    String startIndexSimpleXML(String typeName, int startIndex, int maxFeatures) {
        String xml =
                "<GetFeature version='1.0.0' xmlns:gml=\"http://www.opengis.net/gml\" startIndex='"
                        + startIndex
                        + "'";
        if (maxFeatures > -1) {
            xml += " maxFeatures='" + maxFeatures + "'";
        }
        xml += ">" + " <Query typeName='" + typeName + "'>" + " </Query>" + "</GetFeature>";
        return xml;
    }

    @Test
    public void testStartIndexMultipleTypes() throws Exception {
        doTestStartIndexMultipleTypes("gs:Fifteen", "gs:Seven");
        doTestStartIndexMultipleTypes("cdf:Fifteen", "cdf:Seven");
        doTestStartIndexMultipleTypes("gs:Fifteen", "cdf:Seven");
        doTestStartIndexMultipleTypes("cdf:Fifteen", "gs:Seven");
    }

    public void doTestStartIndexMultipleTypes(String fifteen, String seven) throws Exception {
        String typeNames = fifteen + "," + seven;
        Document doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.0.0&service=wfs&"
                                + "typename="
                                + typeNames
                                + "&startIndex=10");
        // print(doc);
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("7", "count(//" + seven + ")", doc);

        doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.0.0&service=wfs&"
                                + "typename="
                                + typeNames
                                + "&startIndex=16");
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("6", "count(//" + seven + ")", doc);

        doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.0.0&service=wfs&"
                                + "typename="
                                + typeNames
                                + "&startIndex=10&maxfeatures=5");
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + seven + ")", doc);

        doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.0.0&service=wfs&"
                                + "typename="
                                + typeNames
                                + "&startIndex=10&maxfeatures=6");
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + seven + ")", doc);

        doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.0.0&service=wfs&"
                                + "typename="
                                + typeNames
                                + "&startIndex=25");
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + seven + ")", doc);
    }

    @Test
    public void testStartIndexMultipleTypesPOST() throws Exception {
        doTestStartIndexMultipleTypesPOST("gs:Fifteen", "gs:Seven");
        doTestStartIndexMultipleTypesPOST("cdf:Fifteen", "cdf:Seven");
        doTestStartIndexMultipleTypesPOST("gs:Fifteen", "cdf:Seven");
        doTestStartIndexMultipleTypesPOST("cdf:Fifteen", "gs:Seven");
    }

    public void doTestStartIndexMultipleTypesPOST(String fifteen, String seven) throws Exception {
        Document doc = postAsDOM("wfs", startIndexMultiXML(fifteen, seven, 10, -1));
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("7", "count(//" + seven + ")", doc);

        doc = postAsDOM("wfs", startIndexMultiXML(fifteen, seven, 16, -1));
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("6", "count(//" + seven + ")", doc);

        doc = postAsDOM("wfs", startIndexMultiXML(fifteen, seven, 10, 5));
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + seven + ")", doc);

        doc = postAsDOM("wfs", startIndexMultiXML(fifteen, seven, 10, 6));
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + seven + ")", doc);

        doc = postAsDOM("wfs", startIndexMultiXML(fifteen, seven, 25, -1));
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + seven + ")", doc);
    }

    String startIndexMultiXML(String fifteen, String seven, int startIndex, int maxFeatures) {
        String xml =
                "<GetFeature version=\"1.0.0\" xmlns:gml=\"http://www.opengis.net/gml\" startIndex='"
                        + startIndex
                        + "'";
        if (maxFeatures > -1) {
            xml += " maxFeatures='" + maxFeatures + "'";
        }
        xml +=
                ">"
                        + " <Query typeName='"
                        + fifteen
                        + "'>"
                        + " </Query>"
                        + " <Query typeName='"
                        + seven
                        + "'>"
                        + " </Query>"
                        + "</GetFeature>";
        return xml;
    }

    @Test
    public void testWithFilter() throws Exception {
        doTestWithFilter("gs:Fifteen");
        doTestWithFilter("cdf:Fifteen");
    }

    public void doTestWithFilter(String typeName) throws Exception {
        Document doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.1.0&service=wfs&"
                                + "typename="
                                + typeName
                                + "&startIndex=10");
        // print(doc);
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + typeName + ")", doc);

        doc =
                getAsDOM(
                        "/wfs?request=GetFeature&version=1.1.0&service=wfs&"
                                + "typename="
                                + typeName
                                + "&startIndex=10&maxfeatures=4");
        XMLAssert.assertXpathEvaluatesTo("4", "count(//" + typeName + ")", doc);

        String xml =
                String.format(
                        "<GetFeature version='1.1.0' xmlns:gml='http://www.opengis.net/gml' "
                                + "startIndex='%d' maxFeatures='%d'>"
                                + "<Query typeName = '%s'/>"
                                + "</GetFeature>",
                        10, 100, typeName);

        doc = postAsDOM("wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + typeName + ")", doc);

        xml =
                String.format(
                        "<GetFeature version='1.1.0' xmlns:gml='http://www.opengis.net/gml' "
                                + "xmlns:ogc='http://www.opengis.net/ogc' startIndex='%d' maxFeatures='%d'>"
                                + "<Query typeName = '%s'>"
                                + "  <ogc:Filter>"
                                + "   <ogc:FeatureId fid='%s'></ogc:FeatureId>"
                                + "   <ogc:FeatureId fid='%s'></ogc:FeatureId>"
                                + "   <ogc:FeatureId fid='%s'></ogc:FeatureId>"
                                + "  </ogc:Filter>"
                                + "</Query>"
                                + "</GetFeature>",
                        1, 100, typeName, "Fifteen.3", "Fifteen.4", "Fifteen.5");

        doc = postAsDOM("wfs", xml);

        XMLAssert.assertXpathEvaluatesTo("2", "count(//" + typeName + ")", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "0", "count(//" + typeName + "[@gml:id='Fifteen.3'])", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//" + typeName + "[@gml:id='Fifteen.4'])", doc);
        XMLAssert.assertXpathEvaluatesTo(
                "1", "count(//" + typeName + "[@gml:id='Fifteen.5'])", doc);
    }
}
