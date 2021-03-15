/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.data.complex.AppSchemaDataAccess;
import org.geotools.feature.NameImpl;
import org.geotools.jdbc.JDBCFeatureStore;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Set of tests for simple output formats on complex features requests. */
public class SimpleOutputTest extends StationsAppSchemaTestSupport {

    // stations GML 3.1 namespaces
    protected static final String STATIONS_PREFIX_GML31 = "st_gml31";
    protected static final String STATIONS_URI_GML31 = "http://www.stations_gml31.org/1.0";
    protected static final String MEASUREMENTS_PREFIX_GML31 = "ms_gml31";
    protected static final String MEASUREMENTS_URI_GML31 = "http://www.measurements_gml31.org/1.0";

    // stations GML 3.2 namespaces
    protected static final String STATIONS_PREFIX_GML32 = "st_gml32";
    protected static final String STATIONS_URI_GML32 = "http://www.stations_gml32.org/1.0";
    protected static final String MEASUREMENTS_PREFIX_GML32 = "ms_gml32";
    protected static final String MEASUREMENTS_URI_GML32 = "http://www.measurements_gml32.org/1.0";

    public static final String RULES_METADATAMAP_KEY = "ComplexToSimpleRules";

    protected final XpathEngine xpath = buildKmlXpathEngine();

    @Override
    protected StationsMockData createTestData() {
        return new MockData();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // setup on stations
        activateLayer(STATIONS_PREFIX_GML32, "Station_gml32");
        setupSimpleOutput(STATIONS_PREFIX_GML32, "Station_gml32");
        activateLayer(STATIONS_PREFIX_GML31, "Station_gml31");
        setupSimpleOutput(STATIONS_PREFIX_GML31, "Station_gml31");
    }

    @Test
    public void testCsvOutput() throws Exception {
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }
        String request =
                "st_gml32/wfs?request=GetFeature&version=2.0&typeNames=st_gml32:Station_gml32"
                        + "&outputFormat=csv";
        String result = getAsString(request);
        checkCsv(result);
    }

    @Test
    public void testCsvOutput31() throws Exception {
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }
        String request =
                "st_gml31/wfs?request=GetFeature&version=1.1.0&typeNames=st_gml31:Station_gml31"
                        + "&outputFormat=csv";
        String result = getAsString(request);
        checkCsv(result);
    }

    private void checkCsv(String result) {
        String[] lines = result.split("\\r?\\n");
        assertEquals(4, lines.length);
        assertEquals("FID,mail,name,codeNumber,captureDate,location", lines[0]);
        assertEquals("st.1,station1@stations.org,station1,12,2006-10-25,POINT (1 -1)", lines[1]);
        assertEquals("st.2,station2@stations.org,station2,28,2020-08-12,POINT (-3 -2)", lines[2]);
        assertEquals("st.3,station3@stations.org,station3,0,1998-12-04,POINT (0 0)", lines[3]);
    }

    @Test
    public void testKmlOutput() throws Exception {
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }
        String request =
                "st_gml32/wfs?request=GetFeature&version=2.0&typeNames=st_gml32:Station_gml32"
                        + "&outputFormat=application%2Fvnd.google-earth.kml%2Bxml";
        Document document = getAsDOM(request);
        checkKml(document);
    }

    @Test
    public void testKmlOutput31() throws Exception {
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }
        String request =
                "st_gml31/wfs?request=GetFeature&version=1.1.0&typeNames=st_gml31:Station_gml31"
                        + "&outputFormat=application%2Fvnd.google-earth.kml%2Bxml";
        Document document = getAsDOM(request);
        checkKml(document);
    }

    @Test
    public void testGMLOutput31() throws Exception {
        XpathEngine xpathEngine = XMLUnit.newXpathEngine();
        Map<String, String> finalNamespaces = new HashMap<>();
        // add common namespaces
        finalNamespaces.put("gml", "http://www.opengis.net/gml");
        finalNamespaces.put("wfs", "http://www.opengis.net/wfs");
        finalNamespaces.put("st_gml31", "http://www.stations_gml31.org/1.0");
        // add provided namespaces
        // add namespaces to the xpath engine
        xpathEngine.setNamespaceContext(new SimpleNamespaceContext(finalNamespaces));
        // check if this is an online test with a JDBC based data store
        if (notJdbcBased()) {
            // not a JDBC online test
            return;
        }
        String request =
                "st_gml31/wfs?request=GetFeature&version=1.1.0&typeNames=st_gml31:Station_gml31";
        Document document = getAsDOM(request);
        print(document);
        Element documentElement = document.getDocumentElement();
        // check namespaces and features
        assertEquals(
                "http://www.stations_gml31.org/1.0",
                documentElement.getAttribute("xmlns:st_gml31"));
        assertEquals(
                "http://www.measurements_gml31.org/1.0",
                documentElement.getAttribute("xmlns:ms_gml31"));
        assertEquals(
                "3",
                xpathEngine.evaluate(
                        "count(//wfs:FeatureCollection/gml:featureMember/st_gml31:Station_gml31)",
                        document));
    }

    private void checkKml(Document document) throws XpathException {
        // check fields
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//kml:Document/kml:Schema/kml:SimpleField[@name='mail'])",
                        document));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//kml:Document/kml:Schema/kml:SimpleField[@name='name'])",
                        document));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//kml:Document/kml:Schema/kml:SimpleField[@name='codeNumber'])",
                        document));
        assertEquals(
                "1",
                xpath.evaluate(
                        "count(//kml:Document/kml:Schema/kml:SimpleField[@name='captureDate'])",
                        document));
        // check data
        String featureDataPath =
                "//kml:Document/kml:Folder/kml:Placemark[@id='st.1']/kml:ExtendedData/kml:SchemaData";
        assertEquals(
                "station1@stations.org",
                xpath.evaluate(featureDataPath + "/kml:SimpleData[@name='mail']/text()", document));
        assertEquals(
                "station1",
                xpath.evaluate(featureDataPath + "/kml:SimpleData[@name='name']/text()", document));
        assertEquals(
                "station1@stations.org",
                xpath.evaluate(featureDataPath + "/kml:SimpleData[@name='mail']/text()", document));
        assertEquals(
                "12",
                xpath.evaluate(
                        featureDataPath + "/kml:SimpleData[@name='codeNumber']/text()", document));
        assertEquals(
                "2006-10-25Z",
                xpath.evaluate(
                        featureDataPath + "/kml:SimpleData[@name='captureDate']/text()", document));
    }

    private void setupSimpleOutput(String workspace, String layerName) {
        Catalog catalog = this.getCatalog();
        WorkspaceInfo workspaceInfo = catalog.getWorkspaceByName(workspace);
        // setup layer rule
        String wsName = workspaceInfo.getName();
        Name layerNameComplex = new NameImpl(workspace, layerName);
        LayerInfo layerInfo = catalog.getLayerByName(layerNameComplex);
        MetadataMap metadataMap = layerInfo.getMetadata();
        // create the rules map
        HashMap<String, String> rulesMap = new HashMap<>();
        rulesMap.put("mail", wsName + ":contact/" + wsName + ":Contact/" + wsName + ":mail");
        metadataMap.put(RULES_METADATAMAP_KEY, rulesMap);
        catalog.save(layerInfo);
    }

    private void activateLayer(String workspace, String layerName) {
        Catalog catalog = this.getCatalog();
        // setup layer rule
        Name layerNameComplex = new NameImpl(workspace, layerName);
        LayerInfo layerInfo = catalog.getLayerByName(layerNameComplex);
        layerInfo.getResource().setSimpleConversionEnabled(true);
        catalog.save(layerInfo);
    }

    private static final class MockData extends StationsMockData {

        @Override
        public void addContent() {
            // add GML 3.1 namespaces
            putNamespace(STATIONS_PREFIX_GML31, STATIONS_URI_GML31);
            putNamespace(MEASUREMENTS_PREFIX_GML31, MEASUREMENTS_URI_GML31);
            // add GML 3.2 namespaces
            putNamespace(STATIONS_PREFIX_GML32, STATIONS_URI_GML32);
            putNamespace(MEASUREMENTS_PREFIX_GML32, MEASUREMENTS_URI_GML32);
            // add GML 3.1 feature types
            Map<String, String> gml31Parameters = new HashMap<>();
            gml31Parameters.put("GML_PREFIX", "gml31");
            gml31Parameters.put("GML_PREFIX_UPPER", "GML31");
            gml31Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml");
            gml31Parameters.put(
                    "GML_LOCATION", "http://schemas.opengis.net/gml/3.1.1/base/gml.xsd");
            addAppSchemaFeatureType(
                    STATIONS_PREFIX_GML31,
                    "gml31",
                    "Station_gml31",
                    "/test-data/stations/simpleOutput/stations.xml",
                    gml31Parameters,
                    "/test-data/stations/simpleOutput/stations.xsd",
                    "/test-data/stations/simpleOutput/stations.properties",
                    "/test-data/stations/simpleOutput/measurements.xml",
                    "/test-data/stations/simpleOutput/measurements.xsd",
                    "/test-data/stations/simpleOutput/measurements.properties",
                    "/test-data/stations/simpleOutput/tags.properties");
            // add GML 3.2 feature types
            Map<String, String> gml32Parameters = new HashMap<>();
            gml32Parameters.put("GML_PREFIX", "gml32");
            gml32Parameters.put("GML_PREFIX_UPPER", "GML32");
            gml32Parameters.put("GML_NAMESPACE", "http://www.opengis.net/gml/3.2");
            gml32Parameters.put("GML_LOCATION", "http://schemas.opengis.net/gml/3.2.1/gml.xsd");
            addAppSchemaFeatureType(
                    STATIONS_PREFIX_GML32,
                    "gml32",
                    "Station_gml32",
                    "/test-data/stations/simpleOutput/stations.xml",
                    gml32Parameters,
                    "/test-data/stations/simpleOutput/stations.xsd",
                    "/test-data/stations/simpleOutput/stations.properties",
                    "/test-data/stations/simpleOutput/measurements.xml",
                    "/test-data/stations/simpleOutput/measurements.xsd",
                    "/test-data/stations/simpleOutput/measurements.properties",
                    "/test-data/stations/simpleOutput/tags.properties");
        }
    }

    /**
     * Helper method that checks if this is an online test not based on a JDBC data store.
     *
     * @return TRUE if this is an online test not based on a JDBC data store
     */
    private boolean notJdbcBased() throws Exception {
        // get the App-Schema data store
        FeatureTypeInfo featureTypeInfo = getCatalog().getFeatureTypeByName("Station_gml31");
        DataAccess dataAccess = featureTypeInfo.getStore().getDataStore(null);
        AppSchemaDataAccess appSchemaDataAccess = (AppSchemaDataAccess) dataAccess;
        // get the feature type mapping corresponding to the stations complex feature type
        Name name = new NameImpl("http://www.stations_gml31.org/1.0", "Station_gml31");
        FeatureSource featureSource = appSchemaDataAccess.getMappingByName(name).getSource();
        return !(featureSource instanceof JDBCFeatureStore);
    }

    protected static XpathEngine buildKmlXpathEngine() {
        // build xpath engine
        XpathEngine xpathEngine = XMLUnit.newXpathEngine();
        Map<String, String> finalNamespaces = new HashMap<>();
        // add common namespaces
        finalNamespaces.put("kml", "http://www.opengis.net/kml/2.2");
        finalNamespaces.put("", "http://www.opengis.net/kml/2.2");
        // add provided namespaces
        // add namespaces to the xpath engine
        xpathEngine.setNamespaceContext(new SimpleNamespaceContext(finalNamespaces));
        return xpathEngine;
    }
}
