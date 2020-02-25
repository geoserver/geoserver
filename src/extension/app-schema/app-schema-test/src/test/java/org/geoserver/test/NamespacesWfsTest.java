/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.Map;
import net.opengis.wfs20.StoredQueryDescriptionType;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.StoredQuery;
import org.geoserver.wfs.StoredQueryProvider;
import org.geotools.wfs.v2_0.WFS;
import org.geotools.wfs.v2_0.WFSConfiguration;
import org.geotools.xsd.Parser;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Tests that namespaces are correctly handled by WFS and app-schema when features belonging to
 * different namespaces are chained together.
 */
public final class NamespacesWfsTest extends StationsAppSchemaTestSupport {

    private static final String TEST_STORED_QUERY_ID = "NamespacesTestStoredQuery";

    /* Should return the same result as a GetFeature request against the Station feature type */
    private static final String TEST_STORED_QUERY_DEFINITION =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<wfs:StoredQueryDescription id='"
                    + TEST_STORED_QUERY_ID
                    + "'"
                    + " xmlns:xlink=\"http://www.w3.org/1999/xlink\""
                    + " xmlns:ows=\"http://www.opengis.net/ows/1.1\""
                    + " xmlns:gml=\"${GML_NAMESPACE}\""
                    + " xmlns:wfs=\"http://www.opengis.net/wfs/2.0\""
                    + " xmlns:fes=\"http://www.opengis.net/fes/2.0\">>\n"
                    + "  <wfs:QueryExpressionText\n"
                    + "   returnFeatureTypes='st_${GML_PREFIX}:Station_${GML_PREFIX}'\n"
                    + "   language='urn:ogc:def:queryLanguage:OGC-WFS::WFS_QueryExpression'\n"
                    + "   isPrivate='false'>\n"
                    + "    <wfs:Query typeNames='st_${GML_PREFIX}:Station_${GML_PREFIX}'>\n"
                    + "      <fes:Filter>\n"
                    + "        <fes:PropertyIsEqualTo>\n"
                    + "          <fes:ValueReference>st_${GML_PREFIX}:measurements/ms_${GML_PREFIX}:Measurement_${GML_PREFIX}/ms_${GML_PREFIX}:name</fes:ValueReference>\n"
                    + "          <fes:Literal>wind</fes:Literal>\n"
                    + "        </fes:PropertyIsEqualTo>\n"
                    + "      </fes:Filter>\n"
                    + "    </wfs:Query>\n"
                    + "  </wfs:QueryExpressionText>\n"
                    + "</wfs:StoredQueryDescription>";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // inject a test namespace to check on responses
        addTestNamespaceToCatalog();
    }

    @Test
    public void globalServiceGetFeatureNamespacesWfs11() {
        Document document =
                getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31");
        checkWfs11StationsGetFeatureResult(document);
    }

    @Test
    public void virtualServiceGetFeatureNamespacesWfs11() {
        Document document =
                getAsDOM(
                        "st_gml31/wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31");
        checkWfs11StationsGetFeatureResult(document);
    }

    @Test
    public void globalServiceGetFeatureNamespacesWfs20() {
        Document document =
                getAsDOM("wfs?request=GetFeature&version=2.0&typename=st_gml32:Station_gml32");
        checkWfs20StationsGetFeatureResult(document);
    }

    @Test
    public void virtualServiceGetFeatureNamespacesWfs20() {
        Document document =
                getAsDOM(
                        "st_gml32/wfs?request=GetFeature&version=2.0&typename=st_gml32:Station_gml32");
        checkWfs20StationsGetFeatureResult(document);
    }

    /** * GetPropertyValue tests ** */
    @Test
    public void globalServiceGetPropertyValueNamespacesGml32() {
        Document document =
                getAsDOM(
                        "wfs?request=GetPropertyValue&version=2.0&typename=st_gml32:Station_gml32&valueReference=st_gml32:measurements");
        checkGml32StationsGetPropertyValueResult(document);
    }

    @Test
    public void virtualServiceGetPropertyValueNamespacesGml32() {
        Document document =
                getAsDOM(
                        "st_gml32/wfs?request=GetPropertyValue&version=2.0&typename=st_gml32:Station_gml32&valueReference=st_gml32:measurements");
        checkGml32StationsGetPropertyValueResult(document);
    }

    /** * StoredQuery tests ** */
    @Test
    public void globalServiceStoredQueryNamespacesGml32() throws Exception {
        StoredQueryProvider storedQueryProvider = new StoredQueryProvider(getCatalog());
        try {
            createTestStoredQuery(storedQueryProvider, GML32_PARAMETERS);

            Document document =
                    getAsDOM(
                            "wfs?request=GetFeature&version=2.0&StoredQueryID="
                                    + TEST_STORED_QUERY_ID);
            checkWfs20StationsGetFeatureResult(document);
        } finally {
            storedQueryProvider.removeAll();
            assertTrue(storedQueryProvider.listStoredQueries().size() == 1);
        }
    }

    @Test
    public void virtualServiceStoredQueryNamespacesGml32() throws Exception {
        StoredQueryProvider storedQueryProvider = new StoredQueryProvider(getCatalog());
        try {
            createTestStoredQuery(storedQueryProvider, GML32_PARAMETERS);

            Document document =
                    getAsDOM(
                            "st_gml32/wfs?request=GetFeature&version=2.0&StoredQueryID="
                                    + TEST_STORED_QUERY_ID);
            checkWfs20StationsGetFeatureResult(document);
        } finally {
            storedQueryProvider.removeAll();
            assertTrue(storedQueryProvider.listStoredQueries().size() == 1);
        }
    }

    @Test
    public void globalServiceStoredQueryNamespacesGml31() throws Exception {
        StoredQueryProvider storedQueryProvider = new StoredQueryProvider(getCatalog());
        try {
            createTestStoredQuery(storedQueryProvider, GML31_PARAMETERS);

            Document document =
                    getAsDOM(
                            "wfs?request=GetFeature&version=2.0&outputFormat=gml3&StoredQueryID="
                                    + TEST_STORED_QUERY_ID);
            checkWfs11StationsGetFeatureResult(document);
        } finally {
            storedQueryProvider.removeAll();
            assertTrue(storedQueryProvider.listStoredQueries().size() == 1);
        }
    }

    @Test
    public void virtualServiceStoredQueryNamespacesGml31() throws Exception {
        StoredQueryProvider storedQueryProvider =
                new StoredQueryProvider(getCatalog(), null, true, null);
        try {
            createTestStoredQuery(storedQueryProvider, GML31_PARAMETERS);

            Document document =
                    getAsDOM(
                            "st_gml31/wfs?request=GetFeature&version=2.0&outputFormat=gml3&StoredQueryID="
                                    + TEST_STORED_QUERY_ID);
            checkWfs11StationsGetFeatureResult(document);
        } finally {
            storedQueryProvider.removeAll();
            assertTrue(storedQueryProvider.listStoredQueries().size() == 1);
        }
    }

    private void createTestStoredQuery(
            StoredQueryProvider storedQueryProvider, Map<String, String> parameters)
            throws Exception {
        StoredQueryDescriptionType storedQueryDescriptionType =
                createTestStoredQueryDefinition(parameters);
        StoredQuery result = storedQueryProvider.createStoredQuery(storedQueryDescriptionType);

        assertTrue(storedQueryProvider.listStoredQueries().size() == 2);
        assertThat(result.getName(), is(TEST_STORED_QUERY_ID));
        assertThat(
                storedQueryProvider.getStoredQuery(TEST_STORED_QUERY_ID).getName(),
                is(TEST_STORED_QUERY_ID));
    }

    private StoredQueryDescriptionType createTestStoredQueryDefinition(
            Map<String, String> parameters) throws Exception {
        Parser p = new Parser(new WFSConfiguration());
        p.setRootElementType(WFS.StoredQueryDescriptionType);

        String queryDefinition = substitutePlaceHolders(TEST_STORED_QUERY_DEFINITION, parameters);
        StringReader reader = new StringReader(queryDefinition);
        try {
            return (StoredQueryDescriptionType) p.parse(reader);
        } finally {
            reader.close();
        }
    }

    /**
     * Helper method that just substitutes the provided place holder values in the provided string.
     * Place holders are identified with syntax ${PLACE_HOLDER_NAME}.
     */
    private static String substitutePlaceHolders(
            String string, Map<String, String> placeHolderValues) {
        String processedString = string;
        for (Map.Entry<String, String> placeHolder : placeHolderValues.entrySet()) {
            processedString =
                    processedString.replace(
                            String.format("${%s}", placeHolder.getKey()), placeHolder.getValue());
        }
        return processedString;
    }

    /** Check the result of a WFS 1.1 (GML 3.1) get feature request targeting stations data set. */
    private void checkWfs11StationsGetFeatureResult(Document document) {
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/"
                        + "st_gml31:Station_gml31[@gml:id='st.1']/st_gml31:measurements/ms_gml31:Measurement_gml31[ms_gml31:name='temperature']");
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/"
                        + "st_gml31:Station_gml31[@gml:id='st.1']/st_gml31:location/gml:Point[gml:pos='1 -1']");
    }

    /** Check the result of a WFS 2.0 (GML 3.2) get feature request targeting stations data set. */
    private void checkWfs20StationsGetFeatureResult(Document document) {
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/"
                        + "st_gml32:Station_gml32[@gml:id='st.1']/st_gml32:measurements/ms_gml32:Measurement_gml32[ms_gml32:name='temperature']");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/"
                        + "st_gml32:Station_gml32[@gml:id='st.1']/st_gml32:location/gml:Point[gml:pos='1 -1']");
    }

    /**
     * Check the result of a WFS 2.0 (GML 3.2) get property value request targeting the Station
     * feature type.
     */
    private void checkGml32StationsGetPropertyValueResult(Document document) {
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:ValueCollection/wfs:member/"
                        + "st_gml32:measurements/ms_gml32:Measurement_gml32[ms_gml32:name='temperature']");
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:ValueCollection/wfs:member/"
                        + "st_gml32:measurements/ms_gml32:Measurement_gml32[ms_gml32:name='wind']");
    }

    /**
     * Test a request with two queries (to different featureTypes and namespaces) Checks null
     * prefixes issue on multiple query WFS 2.0.0 GML 3.2 versions
     */
    @Test
    public void testTwoQueriesNamespacesGml32() throws Exception {
        String wfsQuery =
                IOUtils.toString(
                        getClass()
                                .getClassLoader()
                                .getResourceAsStream("test-data/stations/stations_two_queries.xml"),
                        "UTF-8");
        Document document = postAsDOM("wfs", wfsQuery);
        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/wfs:member/wfs:FeatureCollection/wfs:member/st_gml32:Station_gml32");

        checkCount(
                WFS20_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/wfs:member/wfs:FeatureCollection/wfs:member/"
                        + "ms_gml32:Measurement_gml32");
        // check prefixes:
        String output = toString(document);
        assertTrue(output.indexOf("null:Measurement_gml32") < 0);
        assertTrue(output.indexOf("null:Station_gml32") < 0);
        assertTrue(output.indexOf("ms_gml32:Measurement_gml32") > -1);
        assertTrue(output.indexOf("st_gml32:Station_gml32") > -1);
        // check test1 namespace not injected:
        assertTrue(output.indexOf("xmlns:test1=\"http://www.test1.org/test1\"") == -1);
    }

    /**
     * Test a request with two queries (to different featureTypes and namespaces) Checks null
     * prefixes issue on multiple query WFS 1.1.0 GML 3.1 version
     */
    @Test
    public void testTwoQueriesNamespacesGml31() throws Exception {
        String wfsQuery =
                IOUtils.toString(
                        getClass()
                                .getClassLoader()
                                .getResourceAsStream(
                                        "test-data/stations/stations_two_queries_1.1.xml"),
                        "UTF-8");
        Document document = postAsDOM("wfs", wfsQuery);
        String output = toString(document);
        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31");

        checkCount(
                WFS11_XPATH_ENGINE,
                document,
                1,
                "/wfs:FeatureCollection/gml:featureMember/" + "ms_gml31:Measurement_gml31");
        // check prefixes:
        assertTrue(output.indexOf("null:Measurement_gml31") < 0);
        assertTrue(output.indexOf("null:Station_gml31") < 0);
        assertTrue(output.indexOf("ms_gml31:Measurement_gml31") > -1);
        assertTrue(output.indexOf("st_gml31:Station_gml31") > -1);
        // check test1 namespace not injected:
        assertTrue(output.indexOf("xmlns:test1=\"http://www.test1.org/test1\"") == -1);
    }

    @Test
    public void testIsolatedWorkspaceGetFeatureNamespacesWfs11() throws Exception {
        final Catalog catalog = getCatalog();
        WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName("st_gml31");
        workspaceInfo.setIsolated(true);
        catalog.save(workspaceInfo);
        try {
            Document document =
                    getAsDOM(
                            "st_gml31/wfs?request=GetFeature&version=1.1.0&typename=st_gml31:Station_gml31");
            assertEquals(
                    "http://www.measurements_gml31.org/1.0",
                    document.getFirstChild()
                            .getAttributes()
                            .getNamedItemNS(XMLNS, "ms_gml31")
                            .getTextContent());
            assertNull(document.getFirstChild().getAttributes().getNamedItemNS(XMLNS, "test1"));
        } finally {
            workspaceInfo = catalog.getWorkspaceByName("st_gml31");
            workspaceInfo.setIsolated(false);
            catalog.save(workspaceInfo);
        }
    }

    @Test
    public void testIsolatedWorkspaceGetFeatureNamespacesWfs20() throws Exception {
        final Catalog catalog = getCatalog();
        WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName("st_gml32");
        workspaceInfo.setIsolated(true);
        catalog.save(workspaceInfo);
        try {
            Document document =
                    getAsDOM(
                            "st_gml32/wfs?request=GetFeature&version=2.0.0&typename=st_gml32:Station_gml32");
            // check should not return namespace:  xmlns:test1="http://www.test1.org/test1"
            assertEquals(
                    "http://www.measurements_gml32.org/1.0",
                    document.getFirstChild()
                            .getAttributes()
                            .getNamedItemNS(XMLNS, "ms_gml32")
                            .getTextContent());
            assertNull(document.getFirstChild().getAttributes().getNamedItemNS(XMLNS, "test1"));
        } finally {
            workspaceInfo = catalog.getWorkspaceByName("st_gml32");
            workspaceInfo.setIsolated(false);
            catalog.save(workspaceInfo);
        }
    }

    private void addTestNamespaceToCatalog() {
        NamespaceInfoImpl ns1 = new NamespaceInfoImpl();
        ns1.setURI("http://www.test1.org/test1");
        ns1.setPrefix("test1");
        getCatalog().add(ns1);
    }
}
