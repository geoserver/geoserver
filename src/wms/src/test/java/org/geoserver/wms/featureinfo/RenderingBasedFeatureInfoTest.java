/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.junit.After;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.w3c.dom.Document;

public class RenderingBasedFeatureInfoTest extends WMSTestSupport {

    private static final XpathEngine XPATH = XMLUnit.newXpathEngine();

    static {
        // setup XPATH engine namespaces
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("gs", "http://geoserver.org");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("cite", "http://www.opengis.net/cite");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        XPATH.setNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    public static QName GRID = new QName(MockData.CITE_URI, "grid", MockData.CITE_PREFIX);
    public static QName REPEATED = new QName(MockData.CITE_URI, "repeated", MockData.CITE_PREFIX);
    public static QName GIANT_POLYGON =
            new QName(MockData.CITE_URI, "giantPolygon", MockData.CITE_PREFIX);

    // @Override
    // protected String getLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addStyle("box-offset", "box-offset.sld", this.getClass(), getCatalog());
        testData.addStyle(
                "transparent-fill", "transparent-fill.sld", this.getClass(), getCatalog());
        File styles = new File(testData.getDataDirectoryRoot(), "styles");
        File symbol = new File("./src/test/resources/org/geoserver/wms/featureinfo/box-offset.png");
        FileUtils.copyFileToDirectory(symbol, styles);

        testData.addVectorLayer(
                GRID,
                Collections.EMPTY_MAP,
                "grid.properties",
                RenderingBasedFeatureInfoTest.class,
                getCatalog());
        testData.addVectorLayer(
                REPEATED,
                Collections.EMPTY_MAP,
                "repeated_lines.properties",
                RenderingBasedFeatureInfoTest.class,
                getCatalog());
        testData.addVectorLayer(
                GIANT_POLYGON,
                Collections.EMPTY_MAP,
                "giantPolygon.properties",
                SystemTestData.class,
                getCatalog());

        testData.addStyle("ranged", "ranged.sld", this.getClass(), getCatalog());
        testData.addStyle("dynamic", "dynamic.sld", this.getClass(), getCatalog());
        testData.addStyle("symbol-uom", "symbol-uom.sld", this.getClass(), getCatalog());
        testData.addStyle("two-rules", "two-rules.sld", this.getClass(), getCatalog());
        testData.addStyle("two-fts", "two-fts.sld", this.getClass(), getCatalog());
        testData.addStyle("dashed", "dashed.sld", this.getClass(), getCatalog());
        testData.addStyle("dashed-exp", "dashed-exp.sld", this.getClass(), getCatalog());
        testData.addStyle("polydash", "polydash.sld", this.getClass(), getCatalog());
        testData.addStyle("doublepoly", "doublepoly.sld", this.getClass(), getCatalog());
        testData.addStyle("pureLabel", "purelabel.sld", this.getClass(), getCatalog());
        testData.addStyle("transform", "transform.sld", this.getClass(), getCatalog());
    }

    @After
    public void cleanup() {
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        // make sure GetFeatureInfo is not deactivated (this will only update the global service)
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(false);
        getGeoServer().save(wms);
    }

    /** Test hitArea does not overflow out of painted area. */
    @Test
    public void testHitAreaSize() throws Exception {
        GetFeatureInfoRequest request = new GetFeatureInfoRequest();
        GetMapRequest getMapRequest = new GetMapRequest();
        List<MapLayerInfo> layers = new ArrayList<>();

        layers.add(new MapLayerInfo(getCatalog().getLayerByName(MockData.BRIDGES.getLocalPart())));
        getMapRequest.setLayers(layers);
        getMapRequest.setSRS("EPSG:4326");
        getMapRequest.setBbox(new Envelope(0.0001955, 0.0002035, 0.000696, 0.000704));
        getMapRequest.setWidth(100);
        getMapRequest.setHeight(100);
        getMapRequest.setFormat("image/png");
        request.setGetMapRequest(getMapRequest);
        request.setQueryLayers(layers);
        // point is almost centered, but on the other side of the middle point
        request.setXPixel(45);
        request.setYPixel(45);

        FeatureInfoRequestParameters params = new FeatureInfoRequestParameters(request);
        VectorRenderingLayerIdentifier vrli = new VectorRenderingLayerIdentifier(getWMS(), null);
        assertEquals(0, vrli.identify(params, 10).size());
    }

    @Test
    public void testBoxOffset() throws Exception {
        // try the old way clicking in the area of the symbol that is transparent
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        String url =
                "wms?REQUEST=GetFeatureInfo&BBOX=1.9E-4,6.9E-4,2.1E-4,7.1E-4&SERVICE=WMS&INFO_FORMAT=application/json"
                        + "&QUERY_LAYERS=cite%3ABridges&Layers=cite%3ABridges&WIDTH=100&HEIGHT=100"
                        + "&format=image%2Fpng&styles=box-offset&srs=EPSG%3A4326&version=1.1.1&x=50&y=63&feature_count=50";
        JSONObject result1 = (JSONObject) getAsJSON(url);
        // print(result1);
        assertEquals(1, result1.getJSONArray("features").size());

        // the new aware is aware that we're clicking into "nothing"
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        JSONObject result2 = (JSONObject) getAsJSON(url);
        // print(result2);
        assertEquals(0, result2.getJSONArray("features").size());
    }

    @Test
    public void testRangedSize() throws Exception {
        // use a style that has a rule with a large symbolizer, but the point is
        // actually painted with a much smaller one
        String url =
                "wms?REQUEST=GetFeatureInfo&BBOX=0.000196%2C0.000696%2C0.000204%2C0.000704"
                        + "&SERVICE=WMS&INFO_FORMAT=application/json&QUERY_LAYERS=cite%3ABridges&FEATURE_COUNT=50&Layers=cite%3ABridges"
                        + "&WIDTH=100&HEIGHT=100&format=image%2Fpng&styles=ranged&srs=EPSG%3A4326&version=1.1.1&x=49&y=65&feature_count=50";

        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        JSONObject result1 = (JSONObject) getAsJSON(url);
        // print(result1);
        assertEquals(1, result1.getJSONArray("features").size());

        // the new aware is aware that we're clicking into "nothing"
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        JSONObject result2 = (JSONObject) getAsJSON(url);
        // print(result2);
        assertEquals(0, result2.getJSONArray("features").size());
    }

    @Test
    public void testDynamicSize() throws Exception {
        // use a style that has a rule with a attribute dependent size, the old code
        // will fallback on the default size since the actual one is not known
        String baseUrl =
                "wms?REQUEST=GetFeatureInfo"
                        + "&BBOX=0.000196%2C0.000696%2C0.000204%2C0.000704&SERVICE=WMS"
                        + "&INFO_FORMAT=application/json&QUERY_LAYERS=cite%3ABridges&FEATURE_COUNT=50"
                        + "&Layers=cite%3ABridges&WIDTH=100&HEIGHT=100&format=image%2Fpng"
                        + "&styles=dynamic&srs=EPSG%3A4326&version=1.1.1&feature_count=50";

        String url = baseUrl + "&x=49&y=60";

        // the default buffer is not large enough to realize we clicked on the mark
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        JSONObject result1 = (JSONObject) getAsJSON(url);
        // print(result1);
        assertEquals(0, result1.getJSONArray("features").size());

        // the new is aware that we're clicking onto the feature instead
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        JSONObject result2 = (JSONObject) getAsJSON(url);
        // print(result2);
        assertEquals(1, result2.getJSONArray("features").size());

        // go more out of the feature to test search radius
        url = baseUrl + "&x=54&y=50&buffer=10";
        JSONObject result3 = (JSONObject) getAsJSON(url);
        // print(result3);
        assertEquals(1, result3.getJSONArray("features").size());
    }

    @Test
    public void testUom() throws Exception {
        // this results in a very large symbol (the map 8m wide and 100 pixels), but if you
        // don't handle uom, you don't get to know that
        String url =
                "wms?REQUEST=GetFeatureInfo"
                        + "&BBOX=0.000196%2C0.000696%2C0.000204%2C0.000704&SERVICE=WMS"
                        + "&INFO_FORMAT=application/json&QUERY_LAYERS=cite%3ABridges&FEATURE_COUNT=50"
                        + "&Layers=cite%3ABridges&WIDTH=100&HEIGHT=100&format=image%2Fpng"
                        + "&styles=symbol-uom&srs=EPSG%3A4326&version=1.1.1&x=49&y=60&feature_count=50";
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
        JSONObject result = (JSONObject) getAsJSON(url);
        // print(result2);
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testTwoRules() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&styles=two-rules"
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format=application/json&feature_count=50";

        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results when two rules matched the same feature
        // print(result);
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testTwoFeatureTypeStyles() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&styles=two-fts"
                        + "&width=20&height=20&x=10&y=10&info_format=application/json";

        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results when two rules matched the same feature
        // print(result);
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testFillStrokeDashArray() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&styles=polydash"
                        + "&width=20&height=20&x=10&y=10&info_format=application/json";

        // System.out.println("The response iTESTs: " + getAsString(request));
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results when two rules matched the same feature
        // print(result);
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testTransparentFill() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&styles=transparent-fill"
                        + "&width=20&height=20&x=10&y=10&info_format=application/json";

        // System.out.println("The response iTESTs: " + getAsString(request));
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results when two rules matched the same feature
        // print(result);
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testGenericGeometry() throws Exception {
        String layer = getLayerId(MockData.GENERICENTITY);
        String request =
                "wms?REQUEST=GetFeatureInfo&BBOX=-2.73291%2C55.220703%2C8.510254%2C69.720703&SERVICE=WMS"
                        + "&INFO_FORMAT=application/json&QUERY_LAYERS="
                        + layer
                        + "&Layers="
                        + layer
                        + "&WIDTH=397&HEIGHT=512&format=image%2Fpng&styles=line&srs=EPSG%3A4326&version=1.1.1&x=284&y=269";
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get no results
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testDashed() throws Exception {
        String layer = getLayerId(MockData.GENERICENTITY);
        String request =
                "wms?REQUEST=GetFeatureInfo&&BBOX=0.778809%2C45.421875%2C12.021973%2C59.921875&SERVICE=WMS"
                        + "&INFO_FORMAT=application/json&QUERY_LAYERS="
                        + layer
                        + "&Layers="
                        + layer
                        + "&WIDTH=397&HEIGHT=512&format=image%2Fpng&styles=dashed&srs=EPSG%3A4326&version=1.1.1&x=182&y=241";
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get no results
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testDashedWithExpressions() throws Exception {
        String layer = getLayerId(MockData.GENERICENTITY);
        String request =
                "wms?REQUEST=GetFeatureInfo&&BBOX=0.778809%2C45.421875%2C12.021973%2C59.921875&SERVICE=WMS"
                        + "&INFO_FORMAT=application/json&QUERY_LAYERS="
                        + layer
                        + "&Layers="
                        + layer
                        + "&WIDTH=397&HEIGHT=512&format=image%2Fpng&styles=dashed-exp&srs=EPSG%3A4326&version=1.1.1&x=182&y=241";
        JSONObject result = (JSONObject) getAsJSON(request);
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testDoublePoly() throws Exception {
        String layer = getLayerId(GRID);
        String request =
                "wms?REQUEST=GetFeatureInfo&&BBOX=0,0,3,3&SERVICE=WMS"
                        + "&INFO_FORMAT=application/json&FEATURE_COUNT=50&QUERY_LAYERS="
                        + layer
                        + "&Layers="
                        + layer
                        + "&WIDTH=90&HEIGHT=90&format=image%2Fpng&styles=doublepoly&srs=EPSG%3A4326&version=1.1.1&x=36&y=36";
        JSONObject result = (JSONObject) getAsJSON(request);
        // print(result);
        // we used to get two results
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testRepeatedLine() throws Exception {
        String layer = getLayerId(REPEATED);
        String request =
                "wms?REQUEST=GetFeatureInfo&&BBOX=499900,499900,500100,500100&SERVICE=WMS"
                        + "&INFO_FORMAT=application/json&FEATURE_COUNT=50&QUERY_LAYERS="
                        + layer
                        + "&Layers="
                        + layer
                        + "&WIDTH=11&HEIGHT=11&format=image%2Fpng&styles=line&srs=EPSG%3A32615&version=1.1.1&x=5&y=5";
        JSONObject result = (JSONObject) getAsJSON(request);
        // print(result);
        // we used to get two results
        assertEquals(2, result.getJSONArray("features").size());
    }

    @Test
    public void testPureLabelGenericGeometry() throws Exception {
        String layer = getLayerId(MockData.GENERICENTITY);
        String request =
                "wms?REQUEST=GetFeatureInfo&&BBOX=0.778809%2C45.421875%2C12.021973%2C59.921875&SERVICE=WMS"
                        + "&INFO_FORMAT=application/json&QUERY_LAYERS="
                        + layer
                        + "&Layers="
                        + layer
                        + "&WIDTH=397&HEIGHT=512&format=image%2Fpng&styles=pureLabel&srs=EPSG%3A4326&version=1.1.1&x=182&y=241";
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get no results
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testPureLabelPolygon() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&styles=pureLabel"
                        + "&width=20&height=20&x=10&y=10&info_format=application/json";

        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results when two rules matched the same feature
        // print(result);
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testMapWrapping() throws Exception {
        GeoServer gs = getGeoServer();
        WMSInfo wms = gs.getService(WMSInfo.class);
        Boolean original = wms.getMetadata().get(WMS.MAP_WRAPPING_KEY, Boolean.class);
        try {
            // wms.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, Boolean.TRUE);
            wms.getMetadata().put(WMS.MAP_WRAPPING_KEY, Boolean.TRUE);
            gs.save(wms);

            String layer = getLayerId(GIANT_POLYGON);
            String request =
                    "wms?version=1.1.1&bbox=170,-10,190,10&format=image/png"
                            + "&request=GetFeatureInfo&layers="
                            + layer
                            + "&query_layers="
                            + layer
                            + "&styles=polygon"
                            + "&width=100&height=100&x=60&y=0&srs=EPSG:4326&info_format=application/json";

            JSONObject result = (JSONObject) getAsJSON(request);
            // with wrapping enabled we should get the giant polygon on the other side too
            assertEquals(1, result.getJSONArray("features").size());

            String disableRequest = request + "&format_options=mapWrapping:false";
            result = (JSONObject) getAsJSON(disableRequest);
            // with wrapping disabled we should not get any hits
            assertEquals(0, result.getJSONArray("features").size());

            String enableRequest = request + "&format_options=mapWrapping:true";
            result = (JSONObject) getAsJSON(enableRequest);
            // with wrapping enabled we should get the giant polygon on the other side too
            assertEquals(1, result.getJSONArray("features").size());

            wms.getMetadata().put(WMS.MAP_WRAPPING_KEY, Boolean.FALSE);
            gs.save(wms);
            result = (JSONObject) getAsJSON(request);
            // with wrapping disabled we should not get any hit
            assertEquals(0, result.getJSONArray("features").size());

            result = (JSONObject) getAsJSON(disableRequest);
            // with wrapping disabled in the config, the request param should be ignored
            assertEquals(0, result.getJSONArray("features").size());

            result = (JSONObject) getAsJSON(enableRequest);
            // with wrapping disabled in the config, the request param should be ignored
            assertEquals(0, result.getJSONArray("features").size());

        } finally {
            wms.getMetadata().put(WMS.MAP_WRAPPING_KEY, original);
            gs.save(wms);
        }
    }

    /**
     * Tests GEOS-7020: imprecise scale calculation in StreamingRenderer with
     * VectorRenderingLayerIdentifier, due to 1 pixel missing in map size.
     */
    @Test
    public void testCalculatedScale() throws Exception {
        int mapWidth = 1000;
        int mapHeight = 500;
        Envelope mapbbox = new Envelope(-2, 2, -1, 1);
        ReferencedEnvelope mapEnvelope =
                new ReferencedEnvelope(mapbbox, DefaultGeographicCRS.WGS84);

        final HashMap<String, String> hints = new HashMap<String, String>();

        double originalScale =
                RendererUtilities.calculateScale(mapEnvelope, mapWidth, mapHeight, hints);
        double originalOGCScale = RendererUtilities.calculateOGCScale(mapEnvelope, mapWidth, hints);

        final MutableDouble calculatedScale = new MutableDouble(0.0);
        final MutableDouble calculatedOGCScale = new MutableDouble(0.0);

        VectorRenderingLayerIdentifier vrli =
                new VectorRenderingLayerIdentifier(getWMS(), null) {

                    @Override
                    protected GetMapOutputFormat createMapOutputFormat(
                            BufferedImage image, FeatureInfoRenderListener featureInfoListener) {
                        return new AbstractMapOutputFormat("image/png", new String[] {"png"}) {

                            @Override
                            public WebMap produceMap(WMSMapContent mapContent)
                                    throws ServiceException, IOException {
                                // let's capture mapContent for identify purpose, so
                                // that we can store the scale(s), to be verified later
                                try {
                                    ReferencedEnvelope referencedEnvelope =
                                            new ReferencedEnvelope(
                                                    mapContent.getViewport().getBounds(),
                                                    DefaultGeographicCRS.WGS84);
                                    calculatedScale.setValue(
                                            RendererUtilities.calculateScale(
                                                    referencedEnvelope,
                                                    mapContent.getMapWidth(),
                                                    mapContent.getMapHeight(),
                                                    hints));
                                    calculatedOGCScale.setValue(
                                            RendererUtilities.calculateOGCScale(
                                                    referencedEnvelope,
                                                    mapContent.getMapWidth(),
                                                    hints));
                                } catch (TransformException e) {
                                    throw new ServiceException(e);
                                } catch (FactoryException e) {
                                    throw new ServiceException(e);
                                }
                                return null;
                            }

                            @Override
                            public MapProducerCapabilities getCapabilities(String format) {
                                return null;
                            }
                        };
                    }
                };

        GetFeatureInfoRequest request = new GetFeatureInfoRequest();
        GetMapRequest getMapRequest = new GetMapRequest();
        List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();

        layers.add(
                new MapLayerInfo(
                        getCatalog().getLayerByName(MockData.BASIC_POLYGONS.getLocalPart())));
        getMapRequest.setLayers(layers);
        getMapRequest.setSRS("EPSG:4326");
        getMapRequest.setBbox(mapbbox);
        getMapRequest.setWidth(mapWidth);

        getMapRequest.setHeight(mapHeight);
        request.setGetMapRequest(getMapRequest);
        request.setQueryLayers(layers);

        FeatureInfoRequestParameters params = new FeatureInfoRequestParameters(request);
        vrli.identify(params, 10);
        // 1% of error tolerance
        assertEquals(originalScale, calculatedScale.doubleValue(), originalScale * 0.01);
        assertEquals(originalOGCScale, calculatedOGCScale.doubleValue(), originalScale * 0.01);
    }

    @Test
    public void testRenderingTransform() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=image/png"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&styles=transform&transparent=true&srs=EPSG:4326"
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format=application/json&feature_count=50";

        JSONObject result = (JSONObject) getAsJSON(request);

        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testGetFeatureInfoReprojectionWithoutRendering() throws Exception {
        // disable WMS get feature info with rendering
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        // request using EPSG:3857 a layer that uses EPSG:4326
        String url =
                "wms?REQUEST=GetFeatureInfo&BBOX=21.1507032494,76.8104486492,23.3770930655,79.0368384649&SERVICE=WMS"
                        + "&INFO_FORMAT=text/xml; subtype=gml/3.1.1&QUERY_LAYERS=cite%3ABridges&Layers=cite%3ABridges&WIDTH=100&HEIGHT=100"
                        + "&format=image%2Fpng&styles=box-offset&srs=EPSG%3A3857&version=1.1.1&x=50&y=63&feature_count=50";
        Document result = getAsDOM(url);
        // check the response content, the features should have been reproject from EPSG:4326 to
        // EPSG:3857
        String srs =
                XPATH.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "cite:Bridges[@gml:id='Bridges.1107531599613']/cite:the_geom/gml:Point/@srsName",
                        result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("3857"), is(true));
        String rawCoordinates =
                XPATH.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "cite:Bridges[@gml:id='Bridges.1107531599613']/cite:the_geom/gml:Point/gml:pos/text()",
                        result);
        checkCoordinates(rawCoordinates, 0.0001, 22.26389816, 77.92364356);
        // disable feature reprojection
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(true);
        getGeoServer().save(wms);
        // execute the get feature info request
        result = getAsDOM(url);
        // check that features were not reprojected
        srs =
                XPATH.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "cite:Bridges[@gml:id='Bridges.1107531599613']/cite:the_geom/gml:Point/@srsName",
                        result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("4326"), is(true));
        rawCoordinates =
                XPATH.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "cite:Bridges[@gml:id='Bridges.1107531599613']/cite:the_geom/gml:Point/gml:pos/text()",
                        result);
        checkCoordinates(rawCoordinates, 0.0001, 0.0002, 0.0007);
    }

    @Test
    public void testGetFeatureInfoReprojectionWithRendering() throws Exception {
        // request using EPSG:3857 a layer that uses EPSG:4326
        String url =
                "wms?REQUEST=GetFeatureInfo&BBOX=-304226.149584,7404818.42511,947357.141801,10978414.0796&SERVICE=WMS"
                        + "&INFO_FORMAT=text/xml; subtype=gml/3.1.1&QUERY_LAYERS=GenericEntity&Layers=GenericEntity"
                        + "&WIDTH=397&HEIGHT=512&format=image%2Fpng&styles=line&srs=EPSG%3A3857&version=1.1.1&x=284&y=269";
        Document result = getAsDOM(url);
        // check the response content, the features should have been reproject from EPSG:4326 to
        // EPSG:3857
        String srs =
                XPATH.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/@srsName",
                        result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("3857"), is(true));
        String exteriorLinearRing =
                XPATH.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/"
                                + "gml:exterior/gml:LinearRing/gml:posList/text()",
                        result);
        checkCoordinates(
                exteriorLinearRing,
                0.0001,
                0,
                8511908.69220489,
                0,
                9349764.17414691,
                695746.81745796,
                9349764.17414691,
                695746.81745796,
                8511908.69220489,
                0,
                8511908.69220489);
        String interiorLinearRing =
                XPATH.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/"
                                + "gml:interior/gml:LinearRing/gml:posList/text()",
                        result);
        checkCoordinates(
                interiorLinearRing,
                0.0001,
                222638.98158655,
                8741545.4358357,
                222638.98158655,
                8978686.31934769,
                445277.96317309,
                8859142.8005657,
                222638.98158655,
                8741545.4358357);
        // disable feature reprojection
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.setFeaturesReprojectionDisabled(true);
        getGeoServer().save(wms);
        // execute the get feature info request
        result = getAsDOM(url);
        // check that features were not reprojected
        srs =
                XPATH.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/@srsName",
                        result);
        assertThat(srs, notNullValue());
        assertThat(srs.contains("4326"), is(true));
        exteriorLinearRing =
                XPATH.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/"
                                + "gml:exterior/gml:LinearRing/gml:posList/text()",
                        result);
        checkCoordinates(exteriorLinearRing, 0.0001, 0, 60.5, 0, 64, 6.25, 64, 6.25, 60.5, 0, 60.5);
        interiorLinearRing =
                XPATH.evaluate(
                        "//wfs:FeatureCollection/gml:featureMembers/"
                                + "sf:GenericEntity[@gml:id='GenericEntity.f004']/sf:attribut.geom/gml:Polygon/"
                                + "gml:interior/gml:LinearRing/gml:posList/text()",
                        result);
        checkCoordinates(interiorLinearRing, 0.0001, 2, 61.5, 2, 62.5, 4, 62, 2, 61.5);
    }

    /**
     * Helper method that checks if the string represented coordinates correspond to the expected
     * ones. The provided precision will be used to compare the numeric values.
     */
    private void checkCoordinates(String rawCoordinates, double precision, double... expected) {
        assertThat(rawCoordinates, notNullValue());
        rawCoordinates = rawCoordinates.trim();
        String[] coordinates = rawCoordinates.split("\\s");
        assertThat(coordinates.length, is(expected.length));
        for (int i = 0; i < coordinates.length; i++) {
            checkNumberSimilar(coordinates[i], expected[i], precision);
        }
    }
}
