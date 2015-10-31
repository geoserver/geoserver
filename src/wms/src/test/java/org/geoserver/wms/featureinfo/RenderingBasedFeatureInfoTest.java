/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.xml.namespace.QName;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.mutable.MutableDouble;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.GetMapTest;
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
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Envelope;

public class RenderingBasedFeatureInfoTest extends WMSTestSupport {

    public static QName GRID = new QName(MockData.CITE_URI, "grid", MockData.CITE_PREFIX);
    public static QName REPEATED = new QName(MockData.CITE_URI, "repeated", MockData.CITE_PREFIX);
    public static QName GIANT_POLYGON = new QName(MockData.CITE_URI, "giantPolygon",
            MockData.CITE_PREFIX);

    
    // @Override
    // protected String getLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        testData.addStyle("box-offset", "box-offset.sld",this.getClass(), getCatalog());
        File styles = getDataDirectory().findOrCreateStyleDir();
        File symbol = new File("./src/test/resources/org/geoserver/wms/featureinfo/box-offset.png");
        FileUtils.copyFileToDirectory(symbol, styles);
        
        testData.addVectorLayer(GRID, Collections.EMPTY_MAP, "grid.properties",
                RenderingBasedFeatureInfoTest.class, getCatalog());
        testData.addVectorLayer(REPEATED, Collections.EMPTY_MAP, "repeated_lines.properties",
                RenderingBasedFeatureInfoTest.class, getCatalog());
        testData.addVectorLayer(GIANT_POLYGON, Collections.EMPTY_MAP, "giantPolygon.properties",
                GetMapTest.class, getCatalog());
        
        testData.addStyle("ranged", "ranged.sld",this.getClass(), getCatalog());
        testData.addStyle("dynamic", "dynamic.sld",this.getClass(), getCatalog());
        testData.addStyle("symbol-uom", "symbol-uom.sld", this.getClass(), getCatalog());
        testData.addStyle("two-rules", "two-rules.sld", this.getClass(), getCatalog());
        testData.addStyle("two-fts", "two-fts.sld", this.getClass(), getCatalog());
        testData.addStyle("dashed", "dashed.sld",this.getClass(), getCatalog());
        testData.addStyle("polydash", "polydash.sld", this.getClass(), getCatalog());
        testData.addStyle("doublepoly", "doublepoly.sld", this.getClass(), getCatalog());
        testData.addStyle("pureLabel", "purelabel.sld", this.getClass(), getCatalog());
        testData.addStyle("transform", "transform.sld", this.getClass(), getCatalog());
    }
    
    @After 
    public void cleanup() {
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
    }
    
    /**
     * Test hitArea does not overflow out of painted area.
     * @throws Exception
     */
    @Test
    public void  testHitAreaSize()  throws Exception {
        int mapWidth = 100;
        int mapHeight = 100;
        
        
        Envelope mapbbox = new Envelope(0.0001955, 0.0002035, 0.000696, 0.000704);
        
        VectorRenderingLayerIdentifier vrli = new VectorRenderingLayerIdentifier(getWMS(), null) {

            @Override
            protected int getBuffer(int userBuffer) {
                return 3;
            }
            

        };
        
        GetFeatureInfoRequest request = new GetFeatureInfoRequest();
        GetMapRequest getMapRequest = new GetMapRequest();
        List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();
        
        layers.add(new MapLayerInfo(getCatalog().getLayerByName(
                MockData.BRIDGES.getLocalPart())));
        getMapRequest.setLayers(layers);
        getMapRequest.setSRS("EPSG:4326");
        getMapRequest.setBbox(mapbbox);
        getMapRequest.setWidth(mapWidth);
        getMapRequest.setFormat("image/png");
        
        getMapRequest.setHeight(mapHeight);
        request.setGetMapRequest(getMapRequest);
        request.setQueryLayers(layers);
        request.setXPixel(50);
        request.setYPixel(50);
        

        FeatureInfoRequestParameters params = new FeatureInfoRequestParameters(request);
        
        assertEquals(0, vrli.identify(params, 10).size());
    }

    
    @Test
    public void testBoxOffset() throws Exception {
        // try the old way clicking in the area of the symbol that is transparent
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        String url = "wms?REQUEST=GetFeatureInfo&BBOX=1.9E-4,6.9E-4,2.1E-4,7.1E-4&SERVICE=WMS&INFO_FORMAT=application/json"
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
        String url = "wms?REQUEST=GetFeatureInfo&BBOX=0.000196%2C0.000696%2C0.000204%2C0.000704"
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
        String url = "wms?REQUEST=GetFeatureInfo"
                + "&BBOX=0.000196%2C0.000696%2C0.000204%2C0.000704&SERVICE=WMS"
                + "&INFO_FORMAT=application/json&QUERY_LAYERS=cite%3ABridges&FEATURE_COUNT=50"
                + "&Layers=cite%3ABridges&WIDTH=100&HEIGHT=100&format=image%2Fpng"
                + "&styles=dynamic&srs=EPSG%3A4326&version=1.1.1&x=49&y=60&feature_count=50";
        
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
    }
    
    @Test
    public void testUom() throws Exception {
        // this results in a very large symbol (the map 8m wide and 100 pixels), but if you
        // don't handle uom, you don't get to know that
        String url = "wms?REQUEST=GetFeatureInfo"
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
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                + "&request=GetFeatureInfo&layers=" + layer + "&query_layers=" + layer
                + "&styles=two-rules"
                + "&width=20&height=20&x=10&y=10" + "&info_format=application/json&feature_count=50";

        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results when two rules matched the same feature
        // print(result);
        assertEquals(1, result.getJSONArray("features").size());
    }
    
    @Test
    public void testTwoFeatureTypeStyles() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                + "&request=GetFeatureInfo&layers=" + layer + "&query_layers=" + layer
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
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                + "&request=GetFeatureInfo&layers=" + layer + "&query_layers=" + layer
                + "&styles=polydash" + "&width=20&height=20&x=10&y=10&info_format=application/json";

        System.out.println("The response iTESTs: " + getAsString(request));
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results when two rules matched the same feature
        // print(result);
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testGenericGeometry() throws Exception {
    	String layer = getLayerId(MockData.GENERICENTITY);
    	String request = "wms?REQUEST=GetFeatureInfo&BBOX=-2.73291%2C55.220703%2C8.510254%2C69.720703&SERVICE=WMS"
    			+ "&INFO_FORMAT=application/json&QUERY_LAYERS=" + layer + "&Layers=" + layer 
    			+ "&WIDTH=397&HEIGHT=512&format=image%2Fpng&styles=line&srs=EPSG%3A4326&version=1.1.1&x=284&y=269";
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get no results 
        assertEquals(1, result.getJSONArray("features").size());
    }
    
    @Test
    public void testDashed() throws Exception {
    	String layer = getLayerId(MockData.GENERICENTITY);
    	String request = "wms?REQUEST=GetFeatureInfo&&BBOX=0.778809%2C45.421875%2C12.021973%2C59.921875&SERVICE=WMS"
    			+ "&INFO_FORMAT=application/json&QUERY_LAYERS=" + layer + "&Layers=" + layer 
    			+ "&WIDTH=397&HEIGHT=512&format=image%2Fpng&styles=dashed&srs=EPSG%3A4326&version=1.1.1&x=182&y=241";
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get no results 
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testDoublePoly() throws Exception {
        String layer = getLayerId(GRID);
        String request = "wms?REQUEST=GetFeatureInfo&&BBOX=0,0,3,3&SERVICE=WMS"
                + "&INFO_FORMAT=application/json&FEATURE_COUNT=50&QUERY_LAYERS="
                + layer
                + "&Layers="
                + layer
                + "&WIDTH=90&HEIGHT=90&format=image%2Fpng&styles=doublepoly&srs=EPSG%3A4326&version=1.1.1&x=34&y=34";
        JSONObject result = (JSONObject) getAsJSON(request);
        // we used to get two results
        assertEquals(1, result.getJSONArray("features").size());
    }

    @Test
    public void testRepeatedLine() throws Exception {
        String layer = getLayerId(REPEATED);
        String request = "wms?REQUEST=GetFeatureInfo&&BBOX=499900,499900,500100,500100&SERVICE=WMS"
                + "&INFO_FORMAT=application/json&FEATURE_COUNT=50&QUERY_LAYERS="
                + layer
                + "&Layers="
                + layer
                + "&WIDTH=11&HEIGHT=11&format=image%2Fpng&styles=line&srs=EPSG%3A32615&version=1.1.1&x=5&y=5";
        JSONObject result = (JSONObject) getAsJSON(request);
        print(result);
        // we used to get two results
        assertEquals(2, result.getJSONArray("features").size());
    }

    @Test
    public void testPureLabelGenericGeometry() throws Exception {
        String layer = getLayerId(MockData.GENERICENTITY);
        String request = "wms?REQUEST=GetFeatureInfo&&BBOX=0.778809%2C45.421875%2C12.021973%2C59.921875&SERVICE=WMS"
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
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=jpeg"
                + "&request=GetFeatureInfo&layers=" + layer + "&query_layers=" + layer
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
            String request = "wms?version=1.1.1&bbox=170,-10,190,10&format=image/png"
                    + "&request=GetFeatureInfo&layers=" + layer + "&query_layers=" + layer
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
     * Tests GEOS-7020: imprecise scale calculation in StreamingRenderer 
     * with VectorRenderingLayerIdentifier, due to 1 pixel missing
     * in map size. 
     * 
     * @throws Exception
     */
    @Test
    public void testCalculatedScale() throws Exception {
        int mapWidth = 1000;
        int mapHeight = 500;
        Envelope mapbbox = new Envelope(-2, 2, -1, 1);
        ReferencedEnvelope mapEnvelope = new ReferencedEnvelope(mapbbox,
                DefaultGeographicCRS.WGS84);
        
        final HashMap<String, String> hints = new HashMap<String, String>();
        
        double originalScale = RendererUtilities.calculateScale(mapEnvelope, mapWidth, mapHeight, hints);
        double originalOGCScale = RendererUtilities.calculateOGCScale(mapEnvelope, mapWidth, hints);
        
        final MutableDouble calculatedScale = new MutableDouble(0.0);
        final MutableDouble calculatedOGCScale = new MutableDouble(0.0);
        
        VectorRenderingLayerIdentifier vrli = new VectorRenderingLayerIdentifier(getWMS(), null) {

            @Override
            protected GetMapOutputFormat createMapOutputFormat(BufferedImage image,
                    FeatureInfoRenderListener featureInfoListener) {
                return new AbstractMapOutputFormat("image/png", new String[] { "png" }) {

                    @Override
                    public WebMap produceMap(WMSMapContent mapContent) throws ServiceException,
                            IOException {
                        // let's capture mapContent for identify purpose, so
                        // that we can store the scale(s), to be verified later
                        try {
                            ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(mapContent.getViewport().getBounds(),
                                    DefaultGeographicCRS.WGS84);
                            calculatedScale.setValue(RendererUtilities.calculateScale(
                                    referencedEnvelope, mapContent.getMapWidth(),
                                    mapContent.getMapHeight(), hints));
                            calculatedOGCScale.setValue(RendererUtilities.calculateOGCScale(
                                    referencedEnvelope, mapContent.getMapWidth(), hints));
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

        layers.add(new MapLayerInfo(getCatalog().getLayerByName(
                MockData.BASIC_POLYGONS.getLocalPart())));
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
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&format=image/png"
                + "&request=GetFeatureInfo&layers=" + layer
                + "&query_layers=" + layer + "&styles=transform&transparent=true&srs=EPSG:4326"
                + "&width=20&height=20&x=10&y=10" + "&info_format=application/json&feature_count=50";
        
        JSONObject result = (JSONObject) getAsJSON(request);
        
        assertEquals(1, result.getJSONArray("features").size());
    }
}
