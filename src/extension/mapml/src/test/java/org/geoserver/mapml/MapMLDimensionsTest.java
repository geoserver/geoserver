/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.HashMap;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** @author prushforth */
public class MapMLDimensionsTest extends WMSDimensionsTestSupport {
    private XpathEngine xpath;

    @Before
    public void setup() {
        HashMap<String, String> m = new HashMap<>();
        m.put("html", "http://www.w3.org/1999/xhtml");

        NamespaceContext ctx = new SimpleNamespaceContext(m);
        XMLUnit.setXpathNamespaceContext(ctx);
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServerInfo global = getGeoServer().getGlobal();
        // this is necessary because the super.onSetUp sets it to something that
        // is not a URL, or at least it's a relative path which fails for
        // the mapml output tests.
        global.getSettings().setProxyBaseUrl("");
        getGeoServer().save(global);
    }

    @Test
    public void testElevationList() throws Exception {
        Catalog catalog = getCatalog();
        ResourceInfo layerMeta = catalog.getLayerByName(getLayerId(V_TIME_ELEVATION)).getResource();
        assertTrue(layerMeta instanceof FeatureTypeInfo);
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) layerMeta;
        // layer has no dimension yet
        assertNull(typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class));
        setupVectorDimension(
                ResourceInfo.ELEVATION,
                "elevation",
                DimensionPresentation.LIST,
                null,
                UNITS,
                UNIT_SYMBOL);

        // re-query the catalog for the updated info
        typeInfo =
                (FeatureTypeInfo)
                        catalog.getLayerByName(getLayerId(V_TIME_ELEVATION)).getResource();
        // get the diminsion info fromt the metadata map
        DimensionInfo elevationInfo =
                typeInfo.getMetadata().get(ResourceInfo.ELEVATION, DimensionInfo.class);
        // prove it's enabled, but not yet known to mapml
        assertTrue(elevationInfo.isEnabled());

        // get the mapml doc for the layer
        String path = "mapml/" + getLayerId(V_TIME_ELEVATION) + "/osmtile";
        org.w3c.dom.Document doc = getMapML(path);
        // assure us it's actually working as a document
        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='image'][@tref])", doc);
        URL url = new URL(xpath.evaluate("//html:map-link[@rel='image']/@tref", doc));
        // assert that there's no elevation variable nor URL template variable
        HashMap<String, String> vars = parseQuery(url);
        assertNull(vars.get("elevation"));
        assertXpathEvaluatesTo("0", "count(//html:map-select[@name='elevation'])", doc);
        // update the layer metadata to indicate the dimension is known to mapml
        // doesn't test the web interface, but the does test the mechanism
        // the web interface uses to signal dimenion for mapml
        layerMeta = catalog.getLayerByName(getLayerId(V_TIME_ELEVATION)).getResource();
        MetadataMap mm = layerMeta.getMetadata();
        mm.put("mapml.dimension", "elevation");
        catalog.save(layerMeta);

        doc = getMapML(path);
        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='image'][@tref])", doc);
        url = new URL(xpath.evaluate("//html:map-link[@rel='image']/@tref", doc));
        vars = parseQuery(url);
        assertTrue(vars.get("elevation").equalsIgnoreCase("{elevation}"));

        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='query'][@tref])", doc);
        url = new URL(xpath.evaluate("//html:map-link[@rel='query']/@tref", doc));
        vars = parseQuery(url);
        assertTrue(vars.get("elevation").equalsIgnoreCase("{elevation}"));

        assertXpathEvaluatesTo(
                "1", "count(//html:map-select[@name='elevation'][@id='elevation'])", doc);
        assertXpathEvaluatesTo(
                "4",
                "count(//html:map-select[@name='elevation'][@id='elevation']/html:map-option)",
                doc);
    }

    @Test
    public void testTimeList() throws Exception {
        Catalog catalog = getCatalog();
        ResourceInfo layerMeta = catalog.getLayerByName(getLayerId(V_TIME_ELEVATION)).getResource();
        assertTrue(layerMeta instanceof FeatureTypeInfo);
        FeatureTypeInfo typeInfo = (FeatureTypeInfo) layerMeta;
        // layer has no dimension yet
        assertNull(typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class));
        setupVectorDimension(
                ResourceInfo.TIME, "time", DimensionPresentation.LIST, null, null, null);

        // re-query the catalog for the updated info
        typeInfo =
                (FeatureTypeInfo)
                        catalog.getLayerByName(getLayerId(V_TIME_ELEVATION)).getResource();
        // get the diminsion info fromt the metadata map
        DimensionInfo timeInfo = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        // prove it's enabled, but not yet known to mapml
        assertTrue(timeInfo.isEnabled());

        // get the mapml doc for the layer
        String path = "mapml/" + getLayerId(V_TIME_ELEVATION) + "/osmtile";
        org.w3c.dom.Document doc = getMapML(path);
        // assure us it's actually working as a document
        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='image'][@tref])", doc);
        URL url = new URL(xpath.evaluate("//html:map-link[@rel='image']/@tref", doc));
        // assert that there's no elevation variable nor URL template variable
        HashMap<String, String> vars = parseQuery(url);
        assertNull(vars.get("elevation"));
        assertXpathEvaluatesTo("0", "count(//html:map-select[@name='time'])", doc);
        // update the layer metadata to indicate the dimension is known to mapml
        // doesn't test the web interface, but the does test the mechanism
        // the web interface uses to signal dimenion for mapml
        layerMeta = catalog.getLayerByName(getLayerId(V_TIME_ELEVATION)).getResource();
        MetadataMap mm = layerMeta.getMetadata();
        mm.put("mapml.dimension", "time");
        catalog.save(layerMeta);

        doc = getMapML(path);
        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='image'][@tref])", doc);
        url = new URL(xpath.evaluate("//html:map-link[@rel='image']/@tref", doc));
        vars = parseQuery(url);
        assertTrue(vars.get("time").equalsIgnoreCase("{time}"));

        assertXpathEvaluatesTo("1", "count(//html:map-link[@rel='query'][@tref])", doc);
        url = new URL(xpath.evaluate("//html:map-link[@rel='query']/@tref", doc));
        vars = parseQuery(url);
        assertTrue(vars.get("time").equalsIgnoreCase("{time}"));

        assertXpathEvaluatesTo("1", "count(//html:map-select[@name='time'][@id='time'])", doc);
        assertXpathEvaluatesTo(
                "4", "count(//html:map-select[@name='time'][@id='time']/html:map-option)", doc);
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
        request.addHeader("Accept", "text/mapml");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        String resp = dispatch(request, "UTF-8").getContentAsString();
        return dom(new ByteArrayInputStream(resp.getBytes()), true);
    }
    /**
     * Executes a request using the GET method and returns the result as an MapML document.
     *
     * @param path The portion of the request after the context, example:
     * @return A result of the request parsed into a dom.
     */
    protected org.w3c.dom.Document getMapML(final String path) throws Exception {
        MockHttpServletRequest request = createRequest(path, false);
        request.addHeader("Accept", "text/mapml");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        String resp = dispatch(request, "UTF-8").getContentAsString();
        return dom(new ByteArrayInputStream(resp.getBytes()), true);
    }

    private HashMap<String, String> parseQuery(URL url) {
        String[] variableValues = url.getQuery().split("&");
        HashMap<String, String> vars = new HashMap<>(variableValues.length);
        // int i = variableValues.length;
        for (String variableValue : variableValues) {
            String[] varValue = variableValue.split("=");
            vars.put(varValue[0], varValue.length == 2 ? varValue[1] : "");
        }
        return vars;
    }
}
