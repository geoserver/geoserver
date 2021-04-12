/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import org.custommonkey.xmlunit.NamespaceContext;
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
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeodeticCRS;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;

/** @author prushfor */
public class MapMLGetFeatureOutputFormatTest extends WFSTestSupport {
    private XpathEngine xpath;

    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        HashMap<String, String> m = new HashMap<>();
        m.put("html", "http://www.w3.org/1999/xhtml/");

        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);
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
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("0", "count(//html:link[@rel='license'])", doc);
        assertXpathEvaluatesTo("0", "count(//html:link[@rel='license']/@href)", doc);
        assertXpathEvaluatesTo("0", "count(//html:link[@rel='license']/@title)", doc);

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
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("1", "count(//html:link[@rel='license'])", doc);
        assertXpathEvaluatesTo(licenseLink, "//html:link[@rel='license']/@href", doc);
        assertXpathEvaluatesTo(licenseTitle, "//html:link[@rel='license']/@title", doc);

        layerInfo = getFeatureTypeInfo(SystemTestData.FIFTEEN);
        layerMeta = layerInfo.getMetadata();
        layerMeta.remove("mapml.licenseLink");
        layerMeta.put("mapml.licenseTitle", "Test License Title");
        getCatalog().save(layerInfo);

        doc = getMapML("wfs", vars);
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("1", "count(//html:link[@rel='license'])", doc);
        assertXpathEvaluatesTo("0", "count(//html:link[@rel='license']/@href)", doc);
        assertXpathEvaluatesTo(licenseTitle, "//html:link[@rel='license']/@title", doc);
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
                assertEquals("mapml", doc.getDocumentElement().getNodeName());
                assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
                assertXpathEvaluatesTo(
                        "1", "count(//html:meta[@name='cs'][@content='" + cs + "'])", doc);
                assertXpathEvaluatesTo("1", "count(//html:meta[@name='projection'])", doc);
                TiledCRSParams tcrs = lookupTCRS(code);
                CoordinateReferenceSystem crs = CRS.decode(code);
                String cite = (crs instanceof GeodeticCRS) ? "MapML:" : "";
                String proj = tcrs == null ? cite + code : tcrs.getName();
                assertXpathEvaluatesTo(
                        "1", "count(//html:meta[@name='projection'][@content='" + proj + "')", doc);
                assertXpathEvaluatesTo("1", "count(//html:meta[@name='extent'])", doc);
                String extent = xpath.evaluate("//html:meta[@name='extent']/@content", doc);
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
    /**
     * @param crsCode - an official CRS code / srsName to look up
     * @return the TCRS corresponding to the crsCode, long or short, or null if not found
     */
    private TiledCRSParams lookupTCRS(String crsCode) {
        return TiledCRSConstants.tiledCRSDefinitions.getOrDefault(
                crsCode, TiledCRSConstants.tiledCRSBySrsName.get(crsCode));
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
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("0", "count(//html:featurecaption)", doc);

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
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("2", "count(//html:featurecaption)", doc);
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
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("0", "count(//html:featurecaption)", doc);

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
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:featurecaption)", doc);
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
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("0", "count(//html:featurecaption)", doc);

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
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
        assertXpathEvaluatesTo("2", "count(//html:featurecaption)", doc);
    }

    @Test
    public void testMapMLOutputFormatCoordinates() throws Exception {
        HashMap<String, String> vars = new HashMap<>();
        vars.put("service", "wfs");
        vars.put("version", "1.0");
        vars.put("request", "GetFeature");
        vars.put("typename", "cdf:Fifteen");
        vars.put("outputFormat", "MAPML");
        vars.put("srsName", "urn:x-ogc:def:crs:EPSG:3978");

        Document doc = getMapML("wfs", vars);
        assertEquals("mapml", doc.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("1", "count(//html:mapml)", doc);
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
