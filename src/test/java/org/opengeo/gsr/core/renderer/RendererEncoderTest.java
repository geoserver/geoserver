package org.opengeo.gsr.core.renderer;

import net.sf.json.util.JSONBuilder;
import net.sf.json.util.JSONStringer;

import org.geoserver.catalog.StyleInfo;
import org.junit.Test;
import org.opengeo.gsr.resource.ResourceTest;
import org.opengis.style.Style;

import static org.junit.Assert.*;

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

}
