/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.tcrs;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.mapml.MapMLConstants;
import org.geoserver.mapml.MapMLTestSupport;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class CustomTiledCRSTest extends MapMLTestSupport {

    protected XpathEngine xpath;

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
    }

    @Before
    public void setup() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServer gs = getGeoServer();
        GeoServerInfo global = gs.getGlobal();
        Catalog catalog = gs.getCatalog();
        addGridSets(gs, catalog, global);
    }

    public static void addGridSets(GeoServer gs, Catalog catalog, GeoServerInfo global) {
        // add UTM31 Gridset to GWC
        GridSetBroker broker = GWC.get().getGridSetBroker();
        broker.addGridSet(createUtm31NGridset());
        broker.addGridSet(createWorldCRS84QuadGridset());
        broker.addGridSet(createCustom3035Gridset());

        // Add 2 custom gridsets to TCRS list
        MetadataMap metadata = global.getSettings().getMetadata();
        ArrayList<String> crsList = new ArrayList<>();
        crsList.add("UTM31WGS84Quad");
        crsList.add("Test3035");
        metadata.put(TiledCRSConstants.TCRS_METADATA_KEY, crsList);
        gs.save(global);

        ResourceInfo layerMeta =
                catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart()).getResource();
        layerMeta.getMetadata().put("mapml.useTiles", false);
        catalog.save(layerMeta);
        TiledCRSConstants.reloadDefinitions();
    }

    private static GridSet createWorldCRS84QuadGridset() {
        double[] scaleDenominators = {
            2.795411320143589E8, 1.3977056600717944E8, 6.988528300358972E7,
            3.494264150179486E7, 1.747132075089743E7, 8735660.375448715,
            4367830.1877243575, 2183915.0938621787, 1091957.5469310894,
            545978.7734655447, 272989.38673277234, 136494.69336638617,
            68247.34668319309, 34123.67334159654, 17061.83667079827,
            8530.918335399136, 4265.459167699568, 2132.729583849784
        };
        String[] scaleNames = {
            "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18"
        };
        return createGridSet(
                "WorldCRS84Quad",
                4326,
                new String[] {"-180", "-90.0", "180", "90"},
                scaleDenominators,
                null,
                111319.49079327358d,
                0.00028d,
                scaleNames,
                true);
    }

    private static GridSet createUtm31NGridset() {
        double[] resolutions = {
            156543.03392804097, 78271.51696402048, 39135.75848201024, 19567.87924100512,
            9783.93962050256, 4891.96981025128, 2445.98490512564, 1222.99245256282,
            611.49622628141, 305.748113140705, 152.8740565703525, 76.43702828517625,
            38.21851414258813, 19.109257071294063, 9.554628535647032, 4.777314267823516
        };
        return createGridSet(
                "UTM31WGS84Quad",
                32631,
                new String[] {"166021.44", "0.0", "833978.56", "9329005.18"},
                null,
                resolutions,
                1.0d,
                0.00028d,
                null,
                false);
    }

    private static GridSet createCustom3035Gridset() {
        double[] resolutions = {
            17578.125,
            8789.0625,
            4394.53125,
            2197.265625,
            1098.6328125,
            549.31640625,
            274.658203125,
            137.3291015625,
            68.66455078125,
            34.332275390625,
            17.1661376953125,
            8.58306884765625,
            4.291534423828125,
            2.1457672119140625,
            1.0728836059570312,
            0.5364418029785156
        };
        return createGridSet(
                "Test3035",
                3035,
                new String[] {"2000000.00", "1000000.0", "6500000.0", "5500000.0"},
                null,
                resolutions,
                1.0d,
                0.00028d,
                null,
                false);
    }

    private static GridSet createGridSet(
            String gridSetName,
            int code,
            String[] bbox,
            double[] scaleDenominators,
            double[] resolutions,
            double metersPerUnit,
            double pixelSize,
            String[] scaleNames,
            boolean yFirst) {
        GridSet gridSet;
        Node gridSetNode;
        try {
            // Create a new DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Create a new Document
            Document doc = builder.newDocument();

            // Create root element 'gridSet'
            gridSetNode = doc.createElement("gridSet");
            doc.appendChild(gridSetNode);

            // Add 'name' element
            Node name = doc.createElement("name");
            name.appendChild(doc.createTextNode(gridSetName));
            gridSetNode.appendChild(name);

            // Add 'srs' element
            Node srs = doc.createElement("srs");
            Node number = doc.createElement("number");
            number.appendChild(doc.createTextNode(Integer.toString(code)));
            srs.appendChild(number);
            gridSetNode.appendChild(srs);

            // Add 'extent' element
            Node extent = doc.createElement("extent");
            Node coords = doc.createElement("coords");
            for (String d : bbox) {
                Node coordDouble = doc.createElement("double");
                coordDouble.appendChild(doc.createTextNode(d));
                coords.appendChild(coordDouble);
            }
            extent.appendChild(coords);
            gridSetNode.appendChild(extent);

            // Add 'alignTopLeft' element
            Node alignTopLeft = doc.createElement("alignTopLeft");
            alignTopLeft.appendChild(doc.createTextNode("false"));
            gridSetNode.appendChild(alignTopLeft);

            // Add 'scaleDenominators' element
            if (scaleDenominators != null) {
                Node scaleDenominatorsElement = doc.createElement("scaleDenominators");
                for (double scale : scaleDenominators) {
                    Node scaleDouble = doc.createElement("double");
                    scaleDouble.appendChild(doc.createTextNode(Double.toString(scale)));
                    scaleDenominatorsElement.appendChild(scaleDouble);
                }
                gridSetNode.appendChild(scaleDenominatorsElement);
            }
            if (resolutions != null) {
                Node resolutionsElement = doc.createElement("resolutions");
                for (double resolution : resolutions) {
                    Node resoulutionDouble = doc.createElement("double");
                    resoulutionDouble.appendChild(doc.createTextNode(Double.toString(resolution)));
                    resolutionsElement.appendChild(resoulutionDouble);
                }
                gridSetNode.appendChild(resolutionsElement);
            }

            // Add 'metersPerUnit' element
            Node metersPerUnitElement = doc.createElement("metersPerUnit");
            metersPerUnitElement.appendChild(doc.createTextNode(Double.toString(metersPerUnit)));
            gridSetNode.appendChild(metersPerUnitElement);

            // Add 'pixelSize' element
            Node pixelSizeElement = doc.createElement("pixelSize");
            pixelSizeElement.appendChild(doc.createTextNode(Double.toString(pixelSize)));
            gridSetNode.appendChild(pixelSizeElement);

            // Add 'scaleNames' element
            if (scaleNames != null) {
                Node scaleNamesElement = doc.createElement("scaleNames");
                for (String nameStr : scaleNames) {
                    Node string = doc.createElement("string");
                    string.appendChild(doc.createTextNode(nameStr));
                    scaleNamesElement.appendChild(string);
                }
                gridSetNode.appendChild(scaleNamesElement);
            }

            // Add 'tileHeight' element
            Node tileHeight = doc.createElement("tileHeight");
            tileHeight.appendChild(doc.createTextNode("256"));
            gridSetNode.appendChild(tileHeight);

            // Add 'tileWidth' element
            Node tileWidth = doc.createElement("tileWidth");
            tileWidth.appendChild(doc.createTextNode("256"));
            gridSetNode.appendChild(tileWidth);

            // Add 'yCoordinateFirst' element
            Node yCoordinateFirst = doc.createElement("yCoordinateFirst");
            yCoordinateFirst.appendChild(doc.createTextNode(Boolean.toString(yFirst)));
            gridSetNode.appendChild(yCoordinateFirst);
            gridSet = TiledCRSConstants.parseGridSetElement((org.w3c.dom.Element) gridSetNode);
        } catch (ParserConfigurationException | XPathExpressionException e) {
            throw new RuntimeException(e);
        }
        return gridSet;
    }

    @Test
    public void testGridsetToTiledCRS() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("MapML:UTM31WGS84Quad");
        CoordinateReferenceSystem expected = CRS.decode("EPSG:32631");
        assertFalse(CRS.isTransformationRequired(crs, expected));
        assertTrue(CRS.equalsIgnoreMetadata(crs, expected));
        assertEquals("MapML", crs.getName().getCodeSpace());
        assertTrue(crs.getName().getCode().equalsIgnoreCase("UTM31WGS84Quad"));

        Set<String> codes = CRS.getSupportedCodes("MapML");
        assertTrue(codes.contains("UTM31WGS84Quad"));
    }

    @Test
    public void testTiledCRSOutput() throws Exception {
        String path = "wms?LAYERS=cite:RoadSegments"
                + "&STYLES=&FORMAT="
                + MapMLConstants.MAPML_MIME_TYPE
                + "&SERVICE=WMS&VERSION=1.3.0"
                + "&REQUEST=GetMap"
                + "&SRS=MapML:UTM31WGS84Quad"
                + "&BBOX=166021.44308054057, 0.0, 277438.26352113695, 110597.97252381293"
                + "&WIDTH=150"
                + "&HEIGHT=150"
                + "&format_options="
                + MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION
                + ":image/png";
        Document doc = getMapML(path);
        print(doc);
        String url = xpath.evaluate("//html:map-link[@rel='" + "image" + "']/@tref", doc);
        assertTrue(url.contains("EPSG:32631"));
    }

    @Test
    public void testTiledCRSYXOutput() throws Exception {
        String path = "wms?LAYERS=cite:RoadSegments"
                + "&STYLES=&FORMAT="
                + MapMLConstants.MAPML_MIME_TYPE
                + "&SERVICE=WMS&VERSION=1.3.0"
                + "&REQUEST=GetMap"
                + "&SRS=MapML:Test3035"
                + "&BBOX=3086656.974771753,-2292253.8117892854,3214455.5231642732,-2192552.9770669416"
                + "&WIDTH=150"
                + "&HEIGHT=150"
                + "&format_options="
                + MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION
                + ":image/png";
        Document doc = getMapML(path);
        print(doc);
        String url = xpath.evaluate("//html:map-link[@rel='" + "image" + "']/@tref", doc);
        assertTrue(url.contains("bbox={ymin},{xmin},{ymax},{xmax}"));
    }

    @Test
    public void testTiledCRSOutputHTMLContainsProjectionDefinition() throws Exception {
        String path = "wms?LAYERS=cite:RoadSegments"
                + "&STYLES=&FORMAT="
                + MapMLConstants.MAPML_HTML_MIME_TYPE
                + "&SERVICE=WMS&VERSION=1.1.1"
                + "&REQUEST=GetMap"
                + "&SRS=MapML:UTM31WGS84Quad"
                + "&BBOX=166021.44308054057, 0.0, 277438.26352113695, 110597.97252381293"
                + "&WIDTH=150"
                + "&HEIGHT=150"
                + "&format_options="
                + MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION
                + ":image/png";
        MockHttpServletRequest request = createRequest(path, false);
        request.setMethod("GET");
        request.setContent(new byte[] {});
        String htmlResponse = dispatch(request, "UTF-8").getContentAsString();

        assertNotNull("Html method must return a document", htmlResponse);
        org.jsoup.nodes.Document doc = Jsoup.parse(htmlResponse);
        Element webmapimport = doc.head().select("script").first();
        assertTrue(
                "HTML document script must use mapml.js module",
                webmapimport.attr("src").matches(".*mapml\\.js"));

        Element nextScript = webmapimport.nextElementSibling().nextElementSibling();
        assertNotNull(nextScript);
        String scriptContent = nextScript.html();

        assertTrue(scriptContent.contains("customProjectionDefinition"));
        assertTrue(scriptContent.contains("let map = document.querySelector(\"mapml-viewer\");"));
        assertTrue(scriptContent.contains("map.defineCustomProjection(customProjectionDefinition"));
        // Check the customProjectionDefinition exist
        String projectionPattern = "\"projection\":\\s*\"([^\"]+)\"";
        String resolutionsPattern = "\"resolutions\":\\s*\\[[^]]+\\]";
        String boundsPattern = "\"bounds\":\\s*\\[[^]]+\\]";
        String originPattern = "\"origin\":\\s*\\[[^]]+\\]";
        String proj4stringPattern = "\"proj4string\"\\s*:\\s*\"([^\"]+)\"";

        // Check for the presence of each field
        boolean hasProjection =
                Pattern.compile(projectionPattern).matcher(scriptContent).find();
        boolean hasResolutions =
                Pattern.compile(resolutionsPattern).matcher(scriptContent).find();
        boolean hasBounds =
                Pattern.compile(boundsPattern).matcher(scriptContent).find();
        boolean hasOrigin =
                Pattern.compile(originPattern).matcher(scriptContent).find();
        boolean hasProj4String =
                Pattern.compile(proj4stringPattern).matcher(scriptContent).find();
        assertTrue(hasProjection);
        assertTrue(hasResolutions);
        assertTrue(hasBounds);
        assertTrue(hasOrigin);
        assertTrue(hasProj4String);

        Matcher matcher = Pattern.compile(projectionPattern).matcher(scriptContent);
        if (matcher.find()) {
            String projectionValue = matcher.group(1); // Capture the projection value
            assertEquals("UTM31WGS84Quad", projectionValue);
        }

        Element map = doc.body().select("mapml-viewer").first();
        assertTrue(
                "viewer must have projection set to \"OSMTILE\"",
                map.attr("projection").equalsIgnoreCase("UTM31WGS84QUAD"));
        Element layer = map.getElementsByTag("map-layer").first();
        String zoom = doc.select("mapml-viewer").attr("zoom");
        assertFalse("0".equalsIgnoreCase(zoom));
        String layerSrc = layer.attr("src");
        assertThat(layerSrc, containsString("SRS=MapML%3AUTM31WGS84Quad"));
    }

    protected Document getMapML(final String path) throws Exception {
        MockHttpServletRequest request = createRequest(path, false);
        request.addHeader("Accept", "text/mapml");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        String resp = dispatch(request, "UTF-8").getContentAsString();
        return dom(new ByteArrayInputStream(resp.getBytes()), true);
    }
}
