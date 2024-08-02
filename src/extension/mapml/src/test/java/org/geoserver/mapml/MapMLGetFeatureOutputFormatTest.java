/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.mapml.tcrs.TiledCRSConstants;
import org.geoserver.mapml.tcrs.TiledCRSParams;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeodeticCRS;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;

/** @author prushfor */
public class MapMLGetFeatureOutputFormatTest extends WFSTestSupport {
    private XpathEngine xpath;

    /**
     * Reset layers so that they don't carry on configuration changes across tests
     *
     * @throws IOException
     */
    @Before
    public void resetLayers() throws IOException {
        revertLayer(MockData.FIFTEEN);
    }

    @Override
    protected void setUpNamespaces(Map<String, String> namespaces) {
        super.setUpNamespaces(namespaces);
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
    }

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        xpath = XMLUnit.newXpathEngine();

        WFSInfo wfs = getWFS();
        wfs.setFeatureBounding(false);
        getGeoServer().save(wfs);

        Catalog catalog = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        ResourceInfo ri = catalog.getLayerByName(MockData.STREAMS.getLocalPart()).getResource();

        cb.setupBounds(ri);
        catalog.save(ri);
        FeatureTypeInfo fi = catalog.getFeatureTypeByName(SystemTestData.FIFTEEN.getLocalPart());
        cb.setupBounds(fi);
        catalog.save(fi);
    }

    @Test
    public void testCapabilities() throws Exception {
        Document doc = getAsDOM("wfs?request=GetCapabilities&version=1.0.0");

        // the WFS caps does not have a list of CRS, but we can check the MapML output format
        assertXpathExists(
                "//wfs:WFS_Capabilities/wfs:Capability/wfs:Request/wfs:GetFeature/wfs:ResultFormat/wfs:MAPML",
                doc);
        assertXpathExists(
                "//wfs:WFS_Capabilities/wfs:Capability/wfs:Request/wfs:GetFeature/wfs:ResultFormat/wfs:MAPML-HTML",
                doc);

        doc = getAsDOM("wfs?request=GetCapabilities&version=1.1.0");

        assertXpathExists(
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/"
                        + "ows:Operation[@name='GetFeature']"
                        + "/ows:Parameter[@name='outputFormat']"
                        + "/ows:Value[text()='text/html; subtype=mapml']",
                doc);

        assertXpathExists(
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/"
                        + "ows:Operation[@name='GetFeature']"
                        + "/ows:Parameter[@name='outputFormat']"
                        + "/ows:Value[text()='MAPML']",
                doc);
    }

    @Test
    public void testCapabilitiesWFS200() throws Exception {

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("gs", "http://geoserver.org");
        namespaces.put("soap12", "http://www.w3.org/2003/05/soap-envelope");
        namespaces.put("wfs", "http://www.opengis.net/wfs/2.0");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("fes", "http://www.opengis.net/fes/2.0");
        namespaces.put("gml", "http://www.opengis.net/gml/3.2");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        Document doc = getAsDOM("ows?service=WFS&request=GetCapabilities&acceptversions=2.0.0");
        assertXpathExists(
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/"
                        + "ows:Operation[@name='GetFeature']"
                        + "/ows:Parameter[@name='outputFormat']"
                        + "/ows:AllowedValues/ows:Value[text()='text/html; subtype=mapml']",
                doc);
        assertXpathExists(
                "//wfs:WFS_Capabilities/ows:OperationsMetadata/"
                        + "ows:Operation[@name='GetFeature']"
                        + "/ows:Parameter[@name='outputFormat']"
                        + "/ows:AllowedValues/ows:Value[text()='MAPML']",
                doc);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(getNamespaces()));
    }

    @Test
    public void testMapMLOutputFormatLinks() throws Exception {
        String licenseLink = "https://example.org/license/";
        String licenseTitle = "Test License Title";
        HashMap<String, String> vars = new HashMap<>();
        vars.put("service", "wfs");
        vars.put("version", "1.0.0");
        vars.put("request", "GetFeature");
        vars.put("typename", "cdf:Fifteen");
        vars.put("outputFormat", "MAPML");

        Document doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        assertXpathEvaluatesTo("0", "count(//html:map-link[@rel='license'])", doc);
        assertXpathEvaluatesTo("0", "count(//html:map-link[@rel='license']/@href)", doc);
        assertXpathEvaluatesTo("0", "count(//html:map-link[@rel='license']/@title)", doc);

        FeatureTypeInfo layerInfo = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        MetadataMap layerMeta = layerInfo.getMetadata();
        layerMeta.put("mapml.licenseLink", licenseLink);
        layerMeta.put("mapml.licenseTitle", licenseTitle);
        getCatalog().save(layerInfo);

        layerMeta = getFeatureTypeInfo(SystemTestData.FIFTEEN).getMetadata();
        assertTrue(layerMeta.containsKey("mapml.licenseLink"));
        assertTrue(layerMeta.containsKey("mapml.licenseTitle"));
        assertTrue(layerMeta.get("mapml.licenseLink").toString().equalsIgnoreCase(licenseLink));
        assertTrue(layerMeta.get("mapml.licenseTitle").toString().equalsIgnoreCase(licenseTitle));

        doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='license'])", doc);
        assertXpathEvaluatesTo(licenseLink, "//html:map-link[@rel='license']/@href", doc);
        assertXpathEvaluatesTo(licenseTitle, "//html:map-link[@rel='license']/@title", doc);

        layerInfo = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        layerMeta = layerInfo.getMetadata();
        layerMeta.remove("mapml.licenseLink");
        layerMeta.put("mapml.licenseTitle", "Test License Title");
        getCatalog().save(layerInfo);

        doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='license'])", doc);
        assertXpathEvaluatesTo("0", "count(//html:map-link[@rel='license']/@href)", doc);
        assertXpathEvaluatesTo(licenseTitle, "//html:map-link[@rel='license']/@title", doc);
    }

    @Test
    public void testCoordinateSystemMetaElements() throws Exception {
        HashMap<String, String> vars = new HashMap<>();
        vars.put("service", "wfs");
        vars.put("version", "1.0");
        vars.put("request", "GetFeature");
        vars.put("typename", "cdf:Fifteen");
        vars.put("outputFormat", "MAPML");
        vars.put("srsName", "");

        String[] projectedTcrsCodes = {
            "urn:x-ogc:def:crs:EPSG:3978",
            "urn:x-ogc:def:crs:EPSG:3857",
            "urn:x-ogc:def:crs:EPSG:5936",
            "EPSG:3978",
            "EPSG:3857",
            "EPSG:5936"
        };
        String[] projectedAxes = {"easting", "northing"};
        String[] unprojectedTcrsCodes = {"urn:ogc:def:crs:OGC:1.3:CRS84", "CRS:84"};
        String[] unProjectedAxes = {"latitude", "longitude"};

        String[] unknownGeodeticCrsCodes = {"urn:x-ogc:def:crs:EPSG:4326", "EPSG:4326"};
        String[] unknownProjectedCrsCodes = {"EPSG:32615", "urn:x-ogc:def:crs:EPSG:32615"};
        String[] versions = {"1.0", "1.1", "2.0"};
        for (String version : versions) {
            vars.replace("version", version);
            testMetaElements(projectedTcrsCodes, projectedAxes, vars);
            testMetaElements(unprojectedTcrsCodes, unProjectedAxes, vars);
            testMetaElements(unknownGeodeticCrsCodes, unProjectedAxes, vars);
            testMetaElements(unknownProjectedCrsCodes, projectedAxes, vars);
        }
    }

    private void testMetaElements(String[] codes, String[] axes, HashMap<String, String> vars) {
        Document doc;
        String cs = axes[0].matches("latitude||longitude") ? "gcrs" : "pcrs";
        try {
            for (String code : codes) {
                vars.replace("srsName", code);
                doc = getMapML("wfs", vars);
                assertEquals("mapml-", doc.getDocumentElement().getNodeName());
                assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
                assertXpathEvaluatesTo(
                        "1", "count(//html:map-meta[@name='cs'][@content='" + cs + "'])", doc);
                assertXpathEvaluatesTo("1", "count(//html:map-meta[@name='projection'])", doc);
                TiledCRSParams tcrs = TiledCRSConstants.lookupTCRS(code);
                CoordinateReferenceSystem crs = CRS.decode(code);
                String cite = (crs instanceof GeodeticCRS) ? "MapML:" : "";
                String proj = tcrs == null ? cite + code : tcrs.getName();
                assertXpathEvaluatesTo(
                        "1",
                        "count(//html:map-meta[@name='projection'][@content='" + proj + "')",
                        doc);
                assertXpathEvaluatesTo("1", "count(//html:map-meta[@name='extent'])", doc);
                String extent = xpath.evaluate("//html:map-meta[@name='extent']/@content", doc);
                String[] positions = extent.split(",");
                assertSame(
                        "meta extent must have 4 positions, but this one has: " + positions.length,
                        4,
                        positions.length);
                for (String pos : positions) {
                    String[] nameValue = pos.split("=");
                    String name = nameValue[0];
                    String value = nameValue[1];
                    assertTrue(
                            name.matches(
                                    "top-left-.*||top-right-.*||bottom-left-.*||bottom-right-.*"));
                    String axisPattern = ".*-" + axes[0] + "||" + ".*-" + axes[1];
                    assertTrue(name.matches(axisPattern));
                    try {
                        Double.valueOf(value);
                    } catch (NumberFormatException e) {
                        fail("The value of: " + name + "did not parse as a number");
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    @Test
    public void testFeatureCaptionForAllFeatures() throws Exception {
        HashMap<String, String> vars = new HashMap<>();
        vars.put("service", "wfs");
        vars.put("version", "1.0.0");
        vars.put("request", "GetFeature");
        vars.put("typename", MockData.STREAMS.getLocalPart());
        vars.put("outputFormat", "MAPML");

        FeatureTypeInfo layerInfo = getFeatureTypeInfo(MockData.STREAMS);
        MetadataMap layerMeta = layerInfo.getMetadata();
        layerMeta.clear();
        getCatalog().save(layerInfo);

        Document doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        assertXpathEvaluatesTo("0", "count(//html:map-featurecaption)", doc);

        // test that all features have a caption
        layerInfo = getFeatureTypeInfo(MockData.STREAMS);

        // all features in this layer have an id
        String featureCaptionTemplate = "${FID}";
        layerMeta = layerInfo.getMetadata();
        layerMeta.put("mapml.featureCaption", featureCaptionTemplate);
        getCatalog().save(layerInfo);

        layerMeta = getFeatureTypeInfo(MockData.STREAMS).getMetadata();
        assertTrue(layerMeta.containsKey("mapml.featureCaption"));
        assertTrue(
                layerMeta
                        .get("mapml.featureCaption")
                        .toString()
                        .equalsIgnoreCase(featureCaptionTemplate));

        doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        assertXpathEvaluatesTo("2", "count(//html:map-featurecaption)", doc);
    }

    @Test
    public void testFeatureCaptionIsOptional() throws Exception {
        HashMap<String, String> vars = new HashMap<>();
        vars.put("service", "wfs");
        vars.put("version", "1.0.0");
        vars.put("request", "GetFeature");
        vars.put("typename", MockData.STREAMS.getLocalPart());
        vars.put("outputFormat", "MAPML");

        FeatureTypeInfo layerInfo = getFeatureTypeInfo(MockData.STREAMS);
        MetadataMap layerMeta = layerInfo.getMetadata();
        layerMeta.clear();
        getCatalog().save(layerInfo);

        Document doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        assertXpathEvaluatesTo("0", "count(//html:map-featurecaption)", doc);

        // test that SOME features can have a caption while others may not
        layerInfo = getFeatureTypeInfo(MockData.STREAMS);
        String featureCaptionTemplate = "${NAME}";
        layerMeta = layerInfo.getMetadata();
        layerMeta.put("mapml.featureCaption", featureCaptionTemplate);
        getCatalog().save(layerInfo);

        layerMeta = getFeatureTypeInfo(MockData.STREAMS).getMetadata();
        assertTrue(layerMeta.containsKey("mapml.featureCaption"));
        assertTrue(
                layerMeta
                        .get("mapml.featureCaption")
                        .toString()
                        .equalsIgnoreCase(featureCaptionTemplate));

        doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:map-featurecaption)", doc);
    }

    @Test
    public void testFeatureCaptionTemplateWithPlaceholders() throws Exception {
        HashMap<String, String> vars = new HashMap<>();
        vars.put("service", "wfs");
        vars.put("version", "1.0.0");
        vars.put("request", "GetFeature");
        vars.put("typename", MockData.STREAMS.getLocalPart());
        vars.put("outputFormat", "MAPML");

        FeatureTypeInfo layerInfo = getFeatureTypeInfo(MockData.STREAMS);
        MetadataMap layerMeta = layerInfo.getMetadata();
        layerMeta.clear();
        getCatalog().save(layerInfo);

        Document doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        assertXpathEvaluatesTo("0", "count(//html:map-featurecaption)", doc);

        // test that a string with > 1 ${placeholder} can be processed
        layerInfo = getFeatureTypeInfo(MockData.STREAMS);
        layerMeta = layerInfo.getMetadata();
        String featureCaptionTemplate = "Name: ${NAME}; Feature ID: ${FID}";
        layerMeta.put("mapml.featureCaption", featureCaptionTemplate);
        getCatalog().save(layerInfo);
        assertTrue(layerMeta.containsKey("mapml.featureCaption"));
        assertTrue(
                layerMeta
                        .get("mapml.featureCaption")
                        .toString()
                        .equalsIgnoreCase(featureCaptionTemplate));

        doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        assertXpathEvaluatesTo("2", "count(//html:map-featurecaption)", doc);
    }

    @Test
    public void testMapMLOutputFormatCoordinatesEPSG() throws Exception {
        testMapMLOutputFormatCoordinates("urn:x-ogc:def:crs:EPSG:3978");
    }

    @Test
    public void testMapMLOutputFormatCoordinatesTCRS() throws Exception {
        testMapMLOutputFormatCoordinates("urn:x-ogc:def:crs:MapML:CBMTILE");
    }

    private void testMapMLOutputFormatCoordinates(String srsName) throws Exception {
        FeatureTypeInfo layerInfo = getFeatureTypeInfo(MockData.FIFTEEN);
        MetadataMap layerMeta = layerInfo.getMetadata();
        layerMeta.clear();
        getCatalog().save(layerInfo);
        // why is xpath null here? Need a new instance or we get NPE.
        xpath = XMLUnit.newXpathEngine();

        HashMap<String, String> vars = new HashMap<>();
        vars.put("service", "wfs");
        vars.put("version", "1.0");
        vars.put("request", "GetFeature");
        vars.put("typename", "cdf:Fifteen");
        vars.put("outputFormat", "MAPML");
        vars.put("srsName", srsName);

        Document doc = getMapML("wfs", vars);
        print(doc);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        String coords =
                xpath.evaluate(
                        "//html:map-feature[@id='Fifteen.1']//html:map-coordinates/text()", doc);
        assertEquals(
                "numDecimals unset should return 8 digits of precision",
                "329290.83733147 -5812472.16880127",
                coords);

        layerInfo = getFeatureTypeInfo(MockData.FIFTEEN);
        layerInfo.setNumDecimals(4);
        getCatalog().save(layerInfo);

        doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        coords =
                xpath.evaluate(
                        "//html:map-feature[@id='Fifteen.1']//html:map-coordinates/text()", doc);
        assertEquals(
                "numDecimals=4 should return 4 digits of precision",
                "329290.8373 -5812472.1688",
                coords);

        // be really sure
        layerInfo = getFeatureTypeInfo(MockData.FIFTEEN);
        layerInfo.setNumDecimals(2);
        getCatalog().save(layerInfo);

        doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        coords =
                xpath.evaluate(
                        "//html:map-feature[@id='Fifteen.1']//html:map-coordinates/text()", doc);
        assertEquals(
                "numDecimals=2 should return 2 digits of precision",
                "329290.84 -5812472.17",
                coords);

        // assure that forcedDecimal has effect
        layerInfo = getFeatureTypeInfo(MockData.FIFTEEN);
        layerInfo.setNumDecimals(4);
        layerInfo.setForcedDecimal(false);
        getCatalog().save(layerInfo);

        // coordinates can be very large in this CRS
        vars.replace("srsName", "urn:x-ogc:def:crs:EPSG:3857");

        doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        coords =
                xpath.evaluate(
                        "//html:map-feature[@id='Fifteen.1']//html:map-coordinates/text()", doc);
        assertEquals(
                "With forcedDecimals=false, very large or very small numbers should be returned as scientific notation",
                "-1.03526624685E7 504135.1496",
                coords);

        // check links for alternate projections
        String linkPath =
                "//html:map-head/html:map-link[@rel='alternate' and @projection='%s']/@href";
        assertThat(
                xpath.evaluate(String.format(linkPath, "OSMTILE"), doc),
                containsString("SRSNAME=MapML:OSMTILE"));
        assertThat(
                xpath.evaluate(String.format(linkPath, "CBMTILE"), doc),
                containsString("SRSNAME=MapML:CBMTILE"));
        assertThat(
                xpath.evaluate(String.format(linkPath, "WGS84"), doc),
                containsString("SRSNAME=MapML:WGS84"));
    }

    @Test
    public void testPadWithZeros() throws Exception {
        FeatureTypeInfo layerInfo = getFeatureTypeInfo(MockData.BASIC_POLYGONS);
        layerInfo.setNumDecimals(4);
        layerInfo.setPadWithZeros(true);
        getCatalog().save(layerInfo);
        // why is xpath null here? Need a new instance or we get NPE.
        xpath = XMLUnit.newXpathEngine();

        HashMap<String, String> vars = new HashMap<>();
        vars.put("service", "wfs");
        vars.put("version", "1.0");
        vars.put("request", "GetFeature");
        vars.put(
                "typename",
                MockData.BASIC_POLYGONS.getPrefix() + ":" + MockData.BASIC_POLYGONS.getLocalPart());
        vars.put("outputFormat", "MAPML");
        getCatalog().save(layerInfo);

        Document doc = getMapML("wfs", vars);

        // assure that pad with zeros works
        doc = getMapML("wfs", vars);
        assertEquals("mapml-", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml-)", doc);
        String coords =
                xpath.evaluate(
                        "//html:map-feature[@id='BasicPolygons.1107531493630']//html:map-coordinates/text()",
                        doc);
        assertEquals(
                "numDecimals=4 should return 4 digits of precision including padding with zeros",
                "0.0000 -1.0000 1.0000 0.0000 0.0000 1.0000 -1.0000 0.0000 0.0000 -1.0000",
                coords);
    }

    /**
     * Executes a request using the GET method and returns the result as an MapML document.
     *
     * @param path The portion of the request after the context, example:
     * @param query A map representing kvp to be used by the request.
     * @return A result of the request parsed into a dom.
     */
    protected org.w3c.dom.Document getMapML(final String path, HashMap<String, String> query)
            throws Exception {
        MockHttpServletRequest request = createRequest(path, query);
        request.setMethod("GET");
        request.setContent(new byte[] {});
        String resp = dispatch(request, "UTF-8").getContentAsString();
        return dom(new ByteArrayInputStream(resp.getBytes()), true);
    }
}
