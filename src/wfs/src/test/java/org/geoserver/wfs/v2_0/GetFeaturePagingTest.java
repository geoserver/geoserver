/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.v2_0;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.KvpMap;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.Id;
import org.geotools.api.filter.identity.Identifier;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.v2_0.FESConfiguration;
import org.geotools.xsd.Parser;
import org.junit.Test;
import org.w3c.dom.Document;

public class GetFeaturePagingTest extends WFS20TestSupport {

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        // run all the tests against a store that can do native paging (geopkg) and one that
        // can't (property)
        Catalog cat = getCatalog();
        DataStoreInfo ds = cat.getFactory().createDataStore();
        ds.setName("foo");
        ds.setWorkspace(cat.getDefaultWorkspace());
        ds.setEnabled(true);

        Map<String, Serializable> params = ds.getConnectionParameters();
        params.put("dbtype", "geopkg");
        params.put("database", getTestData().getDataDirectoryRoot().getAbsolutePath() + "/foo.gpkg");
        params.put("read_only", false);
        cat.add(ds);

        SimpleFeatureSource fs1 = getFeatureSource(SystemTestData.FIFTEEN);
        SimpleFeatureSource fs2 = getFeatureSource(SystemTestData.SEVEN);

        DataStore store = (DataStore) ds.getDataStore(null);
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();

        tb.init(fs1.getSchema());
        tb.add("num", Integer.class);
        tb.remove("boundedBy");
        store.createSchema(tb.buildFeatureType());

        tb.init(fs2.getSchema());
        tb.add("num", Integer.class);
        tb.remove("boundedBy");
        store.createSchema(tb.buildFeatureType());

        CatalogBuilder cb = new CatalogBuilder(cat);
        cb.setStore(ds);

        SimpleFeatureStore fs = (SimpleFeatureStore) store.getFeatureSource("Fifteen");
        addFeatures(fs, fs1.getFeatures());

        FeatureTypeInfo ft = cb.buildFeatureType(fs);
        cat.add(ft);

        fs = (SimpleFeatureStore) store.getFeatureSource("Seven");
        addFeatures(fs, fs2.getFeatures());

        ft = cb.buildFeatureType(fs);
        cat.add(ft);
    }

    void addFeatures(SimpleFeatureStore fs, SimpleFeatureCollection features) throws Exception {
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(fs.getSchema());

        DefaultFeatureCollection toAdd = new DefaultFeatureCollection(null, null);
        try (FeatureIterator it = features.features()) {
            SimpleFeature f = null;
            int i = 0;
            while (it.hasNext()) {
                f = (SimpleFeature) it.next();
                b.init(f);
                b.add(f.getAttribute("pointProperty"));
                b.add(i++);
                toAdd.add(b.buildFeature(null));
            }
        }
        fs.addFeatures(toAdd);
    }

    @Test
    public void testSingleType() throws Exception {
        doTestSingleType("gs:Fifteen");
        doTestSingleType("cdf:Fifteen");
    }

    void doTestSingleType(String typeName) throws Exception {

        Document doc = getAsDOM(
                "/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + typeName + "&startIndex=10");
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + typeName + ")", doc);

        doc = getAsDOM(
                "/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + typeName + "&startIndex=16");
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + typeName + ")", doc);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + typeName + "&startIndex=0");
        XMLAssert.assertXpathEvaluatesTo("15", "count(//" + typeName + ")", doc);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&startIndex=1&count=1");
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + ")", doc);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&startIndex=16&count=1");
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

    String startIndexSimpleXML(String typeName, int startIndex, int count) {
        String xml = "<GetFeature version='2.0.0'";

        if (startIndex > -1) {
            xml += " startIndex='" + startIndex + "'";
        }

        if (count > -1) {
            xml += " count='" + count + "'";
        }
        xml += ">" + " <Query typeNames='" + typeName + "'>" + " </Query>" + "</GetFeature>";
        return xml;
    }

    @Test
    public void testStartIndexMultipleTypes() throws Exception {
        doTestStartIndexMultipleTypes("gs:Fifteen", "gs:Seven");
        doTestStartIndexMultipleTypes("cdf:Fifteen", "cdf:Seven");
        // doTestStartIndexMultipleTypes("gs:Fifteen", "cdf:Seven");
        // doTestStartIndexMultipleTypes("cdf:Fifteen", "gs:Seven");
    }

    public void doTestStartIndexMultipleTypes(String fifteen, String seven) throws Exception {
        String typeNames = fifteen + "," + seven;
        Document doc = getAsDOM(
                "/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + typeNames + "&startIndex=10");
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("7", "count(//" + seven + ")", doc);

        doc = getAsDOM(
                "/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + typeNames + "&startIndex=16");
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("6", "count(//" + seven + ")", doc);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeNames
                + "&startIndex=10&count=5");
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + seven + ")", doc);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeNames
                + "&startIndex=10&count=6");
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + seven + ")", doc);

        doc = getAsDOM(
                "/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + typeNames + "&startIndex=25");
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + fifteen + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + seven + ")", doc);
    }

    @Test
    public void testStartIndexMultipleTypesPOST() throws Exception {
        doTestStartIndexMultipleTypesPOST("gs:Fifteen", "gs:Seven");
        doTestStartIndexMultipleTypesPOST("cdf:Fifteen", "cdf:Seven");
        // doTestStartIndexMultipleTypesPOST("gs:Fifteen", "cdf:Seven");
        // doTestStartIndexMultipleTypesPOST("cdf:Fifteen", "gs:Seven");
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

    String startIndexMultiXML(String fifteen, String seven, int startIndex, int count) {
        String xml = "<GetFeature version=\"2.0.0\" startIndex='" + startIndex + "'";
        if (count > -1) {
            xml += " count='" + count + "'";
        }
        xml += ">"
                + " <Query typeNames='"
                + fifteen
                + "'>"
                + " </Query>"
                + " <Query typeNames='"
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
        Document doc = getAsDOM(
                "/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + typeName + "&startIndex=10");
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + typeName + ")", doc);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&startIndex=10&count=4");
        XMLAssert.assertXpathEvaluatesTo("4", "count(//" + typeName + ")", doc);

        String xml = String.format(
                "<GetFeature version='2.0.0' "
                        + "startIndex='%d' count='%d'>"
                        + "<Query typeNames = '%s'/>"
                        + "</GetFeature>",
                10, 100, typeName);

        doc = postAsDOM("wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("5", "count(//" + typeName + ")", doc);

        xml = String.format(
                "<GetFeature version='2.0.0' xmlns:gml='http://www.opengis.net/gml/2.0' "
                        + "xmlns:fes='http://www.opengis.net/fes/2.0' startIndex='%d' count='%d'>"
                        + "<Query typeNames = '%s'>"
                        + "  <fes:Filter>"
                        + "   <fes:ResourceId rid='%s'></fes:ResourceId>"
                        + "   <fes:ResourceId rid='%s'></fes:ResourceId>"
                        + "   <fes:ResourceId rid='%s'></fes:ResourceId>"
                        + "  </fes:Filter>"
                        + "</Query>"
                        + "</GetFeature>",
                1, 100, typeName, "Fifteen.3", "Fifteen.4", "Fifteen.5");

        doc = postAsDOM("wfs", xml);

        XMLAssert.assertXpathEvaluatesTo("2", "count(//" + typeName + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("0", "count(//" + typeName + "[@gml:id='Fifteen.3'])", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + "[@gml:id='Fifteen.4'])", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + "[@gml:id='Fifteen.5'])", doc);
    }

    @Test
    public void testNextPreviousGET() throws Exception {
        doTestNextPreviousGET("gs:Fifteen");
        doTestNextPreviousGET("cdf:Fifteen");
    }

    @Test
    public void testNextPreviousSkipNumberMatchedGET() throws Exception {
        FeatureTypeInfo fti = this.getCatalog().getFeatureTypeByName("Fifteen");
        fti.setSkipNumberMatched(true);
        this.getCatalog().save(fti);
        try {
            assertTrue(fti.getSkipNumberMatched());
            doTestNextPreviousGET("gs:Fifteen");
            doTestNextPreviousGET("cdf:Fifteen");
        } finally {
            fti.setSkipNumberMatched(false);
            this.getCatalog().save(fti);
        }
    }

    public void doTestNextPreviousGET(String typeName) throws Exception {
        Document doc =
                getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + typeName + "&count=5");
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        // Without startindex, results are not sorted and next would be inconsistent,
        // so next is not encoded. See GEOS-5085.
        assertFalse(doc.getDocumentElement().hasAttribute("next"));

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&startIndex=0&count=5");
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        assertStartIndexCount(doc, "next", 5, 5);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&startIndex=5&count=7");
        assertStartIndexCount(doc, "previous", 0, 5);
        assertStartIndexCount(doc, "next", 12, 7);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&startIndex=12&count=7");
        assertStartIndexCount(doc, "previous", 5, 7);
        assertFalse(doc.getDocumentElement().hasAttribute("next"));

        doc = getAsDOM(
                "/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + typeName + "&startIndex=15");
        assertStartIndexCount(doc, "previous", 0, 15);
        assertFalse(doc.getDocumentElement().hasAttribute("next"));
    }

    public void doTestNextPreviousMultipleTypesGET(String fifteen, String seven) throws Exception {
        Document doc =
                getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + fifteen + "&count=5");
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        // Without startindex, results are not sorted and next would be inconsistent,
        // so next is not encoded. See GEOS-5085.
        assertFalse(doc.getDocumentElement().hasAttribute("next"));

        doc = getAsDOM(
                "/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + fifteen + "&startIndex=0&count=5");
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        assertStartIndexCount(doc, "next", 5, 5);

        doc = getAsDOM(
                "/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + fifteen + "&startIndex=5&count=7");
        assertStartIndexCount(doc, "previous", 0, 5);
        assertStartIndexCount(doc, "next", 12, 3);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&" + "typename=" + seven + "&startIndex=15");
        assertStartIndexCount(doc, "previous", 0, 15);
        assertFalse(doc.getDocumentElement().hasAttribute("next"));
    }

    @Test
    public void testNextPreviousPOST() throws Exception {
        doTestNextPreviousPOST("gs:Fifteen");
        doTestNextPreviousPOST("cdf:Fifteen");
    }

    public void doTestNextPreviousPOST(String typeName) throws Exception {

        Document doc = postAsDOM("wfs", startIndexSimpleXML(typeName, -1, 5));
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        // Without startindex, results are not sorted and next would be inconsistent,
        // so next is not encoded. See GEOS-5085.
        assertFalse(doc.getDocumentElement().hasAttribute("next"));

        doc = postAsDOM("wfs", startIndexSimpleXML(typeName, 0, 5));
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        assertStartIndexCount(doc, "next", 5, 5);

        doc = postAsDOM("wfs", startIndexSimpleXML(typeName, 5, 7));
        assertStartIndexCount(doc, "previous", 0, 5);
        assertStartIndexCount(doc, "next", 12, 7);

        doc = postAsDOM("wfs", startIndexSimpleXML(typeName, 15, -1));
        assertStartIndexCount(doc, "previous", 0, 15);
        assertFalse(doc.getDocumentElement().hasAttribute("next"));
    }

    void assertStartIndexCount(Document doc, String att, int startIndex, int count) {
        assertTrue(doc.getDocumentElement().hasAttribute(att));
        String s = doc.getDocumentElement().getAttribute(att);
        String[] kvp = s.split("\\?")[1].split("&");
        int actualStartIndex = -1;
        int actualCount = -1;

        for (String value : kvp) {
            String k = value.split("=")[0];
            String v = value.split("=")[1];
            if ("startIndex".equalsIgnoreCase(k)) {
                actualStartIndex = Integer.parseInt(v);
            }
            if ("count".equalsIgnoreCase(k)) {
                actualCount = Integer.parseInt(v);
            }
        }

        assertEquals(startIndex, actualStartIndex);
        assertEquals(count, actualCount);
    }

    @Test
    public void testNextPreviousLinksPOST() throws Exception {
        doTestNextPreviousLinksPOST("gs:Fifteen");
    }

    public void doTestNextPreviousLinksPOST(String typeName) throws Exception {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        Filter filter = ff.id(new LinkedHashSet<Identifier>( // NOPMD
                Arrays.asList(
                        ff.featureId("Fifteen.5"),
                        ff.featureId("Fifteen.6"),
                        ff.featureId("Fifteen.7"),
                        ff.featureId("Fifteen.8"),
                        ff.featureId("Fifteen.9"))));

        String xml = String.format(
                "<GetFeature version='2.0.0' xmlns:gml='http://www.opengis.net/gml/2.0' "
                        + "xmlns:fes='http://www.opengis.net/fes/2.0' startIndex='%d' count='%d'>"
                        + "<Query typeNames = '%s'>"
                        + "  <fes:Filter>"
                        + "   <fes:ResourceId rid='%s'></fes:ResourceId>"
                        + "   <fes:ResourceId rid='%s'></fes:ResourceId>"
                        + "   <fes:ResourceId rid='%s'></fes:ResourceId>"
                        + "   <fes:ResourceId rid='%s'></fes:ResourceId>"
                        + "   <fes:ResourceId rid='%s'></fes:ResourceId>"
                        + "  </fes:Filter>"
                        + "</Query>"
                        + "</GetFeature>",
                0, 2, typeName, "Fifteen.5", "Fifteen.6", "Fifteen.7", "Fifteen.8", "Fifteen.9");
        Document doc = postAsDOM("wfs", xml);
        XMLAssert.assertXpathEvaluatesTo("2", "count(//" + typeName + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + "[@gml:id='Fifteen.5'])", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + "[@gml:id='Fifteen.6'])", doc);

        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        assertTrue(doc.getDocumentElement().hasAttribute("next"));

        String next = doc.getDocumentElement().getAttribute("next");
        assertKvp(2, 2, typeName, filter, toKvpMap(next));

        doc = getAsDOM(next.substring(next.indexOf("wfs")));
        XMLAssert.assertXpathEvaluatesTo("2", "count(//" + typeName + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + "[@gml:id='Fifteen.7'])", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + "[@gml:id='Fifteen.8'])", doc);

        assertTrue(doc.getDocumentElement().hasAttribute("previous"));
        assertTrue(doc.getDocumentElement().hasAttribute("next"));

        String prev = doc.getDocumentElement().getAttribute("previous");
        assertKvp(0, 2, typeName, filter, toKvpMap(prev));

        next = doc.getDocumentElement().getAttribute("next");
        assertKvp(4, 2, typeName, filter, toKvpMap(next));

        doc = getAsDOM(next.substring(next.indexOf("wfs")));
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + ")", doc);
        XMLAssert.assertXpathEvaluatesTo("1", "count(//" + typeName + "[@gml:id='Fifteen.9'])", doc);

        assertTrue(doc.getDocumentElement().hasAttribute("previous"));
        assertFalse(doc.getDocumentElement().hasAttribute("next"));

        prev = doc.getDocumentElement().getAttribute("previous");
        assertKvp(2, 2, typeName, filter, toKvpMap(prev));
    }

    void assertKvp(int startIndex, int count, String typeName, Filter filter, Map kvp) throws Exception {
        assertEquals(String.valueOf(startIndex), kvp.get("STARTINDEX"));
        assertEquals(String.valueOf(count), kvp.get("COUNT"));
        assertEquals("(" + typeName + ")", URLDecoder.decode((String) kvp.get("TYPENAMES"), "UTF-8"));
        assertNotNull(kvp.get("FILTER"));

        assertFilter(filter, URLDecoder.decode((String) kvp.get("FILTER"), "UTF-8"));
    }

    void assertFilter(Filter expected, String filter) throws Exception {
        filter = filter.substring(1, filter.length() - 1);
        Filter f = (Filter) new Parser(new FESConfiguration()).parse(new ByteArrayInputStream(filter.getBytes()));
        if (expected instanceof Id id1) {
            Set<String> s1 = new HashSet<>();
            for (Identifier id : id1.getIdentifiers()) {
                s1.add(id.toString());
            }
            Set<String> s2 = new HashSet<>();
            for (Identifier id : ((Id) f).getIdentifiers()) {
                s2.add(id.toString());
            }
            assertEquals(s1, s2);
        } else {
            assertEquals(expected, f);
        }
    }

    KvpMap<String, String> toKvpMap(String url) {
        url = url.substring(url.indexOf('?') + 1);
        String[] kvps = url.split("\\&");
        KvpMap<String, String> map = new KvpMap<>();
        for (String kvp : kvps) {
            map.put(kvp.split("=")[0], kvp.split("=")[1]);
        }
        return map;
    }

    @Test
    public void testSortingGET() throws Exception {
        Document dom =
                getAsDOM("wfs?service=WFS&version=2.0.0&request=GetFeature&typeName=gs:Fifteen&sortBy=num ASC&count=1");
        XMLAssert.assertXpathExists("//gs:Fifteen/gs:num[text() = '0']", dom);

        dom = getAsDOM("wfs?service=WFS&version=2.0.0&request=GetFeature&typeName=gs:Fifteen&sortBy=num DESC&count=1");
        XMLAssert.assertXpathExists("//gs:Fifteen/gs:num[text() = '14']", dom);
    }

    @Test
    public void testNextPreviousHitsGET() throws Exception {
        doTestNextPreviousHitsGET("gs:Fifteen");
        doTestNextPreviousHitsGET("cdf:Fifteen");
    }

    public void doTestNextPreviousHitsGET(String typeName) throws Exception {
        Document doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&count=5&resulttype=hits");
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&startIndex=0&count=5&resulttype=hits");
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        assertStartIndexCount(doc, "next", 0, 5);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&startIndex=5&count=7&resulttype=hits");
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        assertStartIndexCount(doc, "next", 0, 7);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&startIndex=12&count=7&resulttype=hits");
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        assertStartIndexCount(doc, "next", 0, 7);

        doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&"
                + "typename="
                + typeName
                + "&startIndex=15&resulttype=hits");
        assertFalse(doc.getDocumentElement().hasAttribute("previous"));
        assertStartIndexCount(doc, "next", 0, -1 /* not there */);
    }

    @Test
    public void testCountZero() throws Exception {
        Document doc = getAsDOM("/wfs?request=GetFeature&version=2.0.0&service=wfs&typename=gs:Fifteen&count=0");
        XMLAssert.assertXpathExists("/wfs:FeatureCollection", doc);
        XMLAssert.assertXpathEvaluatesTo("15", "/wfs:FeatureCollection/@numberMatched", doc);
        XMLAssert.assertXpathEvaluatesTo("0", "/wfs:FeatureCollection/@numberReturned", doc);
    }
}
