package org.opengeo.gsr.core.renderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;

import org.geoserver.catalog.StyleInfo;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLDParser;
import org.geotools.styling.StyleFactory;
import org.junit.Test;
import org.opengeo.gsr.resource.ResourceTest;
import org.opengis.style.Style;

public class RendererEncoderTest extends ResourceTest {
    @Test
    public void testPolygonRendererJsonSchema() throws Exception {
        StyleInfo polygonInfo = getGeoServer().getCatalog().getStyleByName("Lakes");
        assertNotNull(polygonInfo);
        Style polygon = polygonInfo.getStyle();
        assertNotNull(polygon);
        Renderer polygonRenderer =
            StyleEncoder.styleToRenderer((org.geotools.styling.Style) polygon);
        assertNotNull(polygonRenderer);
        JSONBuilder json = new JSONStringer();
        StyleEncoder.encodeRenderer(json, polygonRenderer);
    }
    
    @Test
    public void testLineRenderer() throws Exception {
        StyleInfo lineInfo = getGeoServer().getCatalog().getStyleByName("Streams");
        assertNotNull(lineInfo);
        Style line = lineInfo.getStyle();
        assertNotNull(line);
        Renderer lineRenderer = StyleEncoder.styleToRenderer((org.geotools.styling.Style)line);
        assertNotNull(lineRenderer);
        JSONBuilder json = new JSONStringer();
        StyleEncoder.encodeRenderer(json, lineRenderer);
    }
    
    @Test
    public void testPointRenderer() throws Exception {
        StyleInfo pointInfo = getGeoServer().getCatalog().getStyleByName("Buildings");
        assertNotNull(pointInfo);
        Style point = pointInfo.getStyle();
        assertNotNull(point);
        Renderer pointRenderer = StyleEncoder.styleToRenderer((org.geotools.styling.Style)point);
        assertNotNull(point);
        JSONBuilder json = new JSONStringer();
        StyleEncoder.encodeRenderer(json, pointRenderer);
    }
    
    private Renderer parseAndConvertToRenderer(String sldPath) throws Exception {
        StyleFactory factory = CommonFactoryFinder.getStyleFactory();
        SLDParser parser = new SLDParser(factory, getClass().getResource(sldPath));
        org.geotools.styling.Style sld = parser.readXML()[0];
        return StyleEncoder.styleToRenderer((org.geotools.styling.Style) sld);
    }
    
    @Test
    public void testIconRenderer() throws Exception {
        StyleFactory factory = CommonFactoryFinder.getStyleFactory();
        SLDParser parser = new SLDParser(factory, getClass().getResource("mark.sld"));
        org.geotools.styling.Style sld = parser.readXML()[0];
        JSONBuilder json = new JSONStringer();
        Renderer renderer = StyleEncoder.styleToRenderer((org.geotools.styling.Style) sld);
        assertNotNull(renderer);
        StyleEncoder.encodeRenderer(json, renderer);
        JSONObject object = JSONObject.fromObject(json.toString());
        JSONObject symbol = object.getJSONObject("symbol");
        String url = symbol.getString("url");
        String contentType = symbol.getString("contentType");
        int width = symbol.getInt("width");
        int height = symbol.getInt("height");
        assertTrue(url.endsWith("example.jpg"));
        assertEquals("image/jpeg", contentType);
        assertEquals(64, width);
        assertEquals(64, height);
    }

    @Test
    public void testClassBreaks() throws Exception {
    	Renderer renderer = parseAndConvertToRenderer("earthquakes.sld");
    	assertTrue(renderer.toString(), renderer instanceof ClassBreaksRenderer);
    	renderer = parseAndConvertToRenderer("hnd_bridges_graduated.sld");
    	assertTrue(renderer.toString(), renderer instanceof ClassBreaksRenderer);
    }
    
    @Test
    public void testUniqueValues() throws Exception {
    	Renderer renderer = parseAndConvertToRenderer("hnd_cemeteries_categorized.sld");
    	assertTrue(renderer.toString(), renderer instanceof UniqueValueRenderer);
    }
}
