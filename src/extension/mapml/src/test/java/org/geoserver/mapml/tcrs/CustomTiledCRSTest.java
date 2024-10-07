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
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSetFactory;
import org.geowebcache.grid.SRS;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;

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
        addBuiltinGridSet(gs, catalog, global);
    }

    public static void addBuiltinGridSet(GeoServer gs, Catalog catalog, GeoServerInfo global) {
        // add UTM31 Gridset to GWC
        GridSetBroker broker = GWC.get().getGridSetBroker();
        broker.addGridSet(createUtm31NGridset());

        // Add 2 custom gridsets to TCRS list
        MetadataMap metadata = global.getSettings().getMetadata();
        ArrayList<String> crsList = new ArrayList<>();
        crsList.add("UTM31WGS84Quad");
        metadata.put(TiledCRSConstants.TCRS_METADATA_KEY, crsList);
        gs.save(global);

        ResourceInfo layerMeta =
                catalog.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart()).getResource();
        layerMeta.getMetadata().put("mapml.useTiles", false);
        catalog.save(layerMeta);
        TiledCRSConstants.reloadDefinitions();
    }

    public static GridSet createUtm31NGridset() {
        SRS srs = SRS.getSRS(32631);
        BoundingBox bbox = new BoundingBox(166021.44, 0.0, 833978.56, 9329005.18); // In meters
        double[] resolutions = {
            156543.03392804097, 78271.51696402048, 39135.75848201024, 19567.87924100512,
            9783.93962050256, 4891.96981025128, 2445.98490512564, 1222.99245256282,
            611.49622628141, 305.748113140705, 152.8740565703525, 76.43702828517625,
            38.21851414258813, 19.109257071294063, 9.554628535647032, 4.777314267823516
        };

        double[] scaleDenoms = null;
        Double metersPerUnit = 1.0;
        double pixelSize = 0.00028;
        String[] scaleNames = null;

        int tileWidth = 256;
        int tileHeight = 256;
        boolean yCoordinateFirst = false;
        GridSet gridSet =
                GridSetFactory.createGridSet(
                        "UTM31WGS84Quad", // GridSet name
                        srs, // Spatial reference system (EPSG:32631)
                        bbox, // Bounding box
                        true, // Align top-left
                        resolutions, // Resolutions (meters per pixel)
                        scaleDenoms, // Scale denominators (can be null)
                        metersPerUnit, // Meters per unit
                        pixelSize, // Pixel size in meters (0.00028 for WMS)
                        scaleNames, // Scale names (optional, can be null)
                        tileWidth, // Tile width (256 pixels)
                        tileHeight, // Tile height (256 pixels)
                        yCoordinateFirst // Whether Y-coordinate comes first (false)
                        );

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
        String path =
                "wms?LAYERS=cite:RoadSegments"
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
    public void testTiledCRSOutputHTMLContainsProjectionDefinition() throws Exception {
        String path =
                "wms?LAYERS=cite:RoadSegments"
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
        boolean hasProjection = Pattern.compile(projectionPattern).matcher(scriptContent).find();
        boolean hasResolutions = Pattern.compile(resolutionsPattern).matcher(scriptContent).find();
        boolean hasBounds = Pattern.compile(boundsPattern).matcher(scriptContent).find();
        boolean hasOrigin = Pattern.compile(originPattern).matcher(scriptContent).find();
        boolean hasProj4String = Pattern.compile(proj4stringPattern).matcher(scriptContent).find();
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
