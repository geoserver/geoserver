package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;

import java.io.File;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.After;
import org.junit.Test;

public class RenderingBasedFeatureInfoTest extends WMSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        testData.addStyle("box-offset", "box-offset.sld",this.getClass(), getCatalog());
        File styles = getDataDirectory().findOrCreateStyleDir();
        File symbol = new File("./src/test/resources/org/geoserver/wms/featureinfo/box-offset.png");
        FileUtils.copyFileToDirectory(symbol, styles);
        
        testData.addStyle("ranged", "ranged.sld",this.getClass(), getCatalog());
        testData.addStyle("dynamic", "dynamic.sld",this.getClass(), getCatalog());
    }
    
    @After 
    public void cleanup() {
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = true;
    }
    
    @Test
    public void testBoxOffset() throws Exception {
        // try the old way clicking in the area of the symbol that is transparent
        VectorRenderingLayerIdentifier.RENDERING_FEATUREINFO_ENABLED = false;
        String url = "wms?REQUEST=GetFeatureInfo&BBOX=1.9E-4,6.9E-4,2.1E-4,7.1E-4&SERVICE=WMS&INFO_FORMAT=application/json"
                + "&QUERY_LAYERS=cite%3ABridges&Layers=cite%3ABridges&WIDTH=100&HEIGHT=100"
                + "&format=image%2Fpng&styles=box-offset&srs=EPSG%3A4326&version=1.1.1&x=50&y=63";
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
        + "&WIDTH=100&HEIGHT=100&format=image%2Fpng&styles=ranged&srs=EPSG%3A4326&version=1.1.1&x=49&y=65";
        
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
                + "&styles=dynamic&srs=EPSG%3A4326&version=1.1.1&x=49&y=60";
        
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
}
