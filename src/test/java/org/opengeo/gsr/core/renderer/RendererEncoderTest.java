package org.opengeo.gsr.core.renderer;

import net.sf.json.util.JSONBuilder;

import org.geoserver.catalog.StyleInfo;
import org.junit.Test;
import org.opengeo.gsr.resource.ResourceTest;
import org.opengis.style.Style;

public class RendererEncoderTest extends ResourceTest {
    @Test
    public void testSimpleRendererJsonSchema() throws Exception {
        StyleInfo polygonInfo = getGeoServer().getCatalog().getStyleByName("point");
        assertNotNull(polygonInfo);
        Style polygon = polygonInfo.getStyle();
        assertNotNull(polygon);
        Renderer polygonRenderer =
            StyleEncoder.styleToRenderer((org.geotools.styling.Style) polygon);
        assertNotNull(polygonRenderer);
        JSONBuilder json = new net.sf.json.util.JSONStringer();
        StyleEncoder.encodeRenderer(json, polygonRenderer);
//        fail(json.toString());
    }
}
