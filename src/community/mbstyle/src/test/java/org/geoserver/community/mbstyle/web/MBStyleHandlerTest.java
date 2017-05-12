package org.geoserver.community.mbstyle.web;

import org.geoserver.catalog.Styles;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.rest.format.MediaTypes;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.styling.*;
import org.junit.Test;
import org.restlet.data.MediaType;

import java.io.IOException;
import static org.junit.Assert.*;


public class MBStyleHandlerTest extends GeoServerSystemTestSupport {

    @Test
    public void testParseThroughStyles() throws IOException {
        String mbstyle = "{\"layers\": [{\n" +
                "    \"type\": \"line\",\n" +
                "    \"paint\": {\n" +
                "        \"line-color\": \"#0099ff\",\n" +
                "        \"line-width\": 10,\n" +
                "    }\n" +
                "}]}";
        StyledLayerDescriptor sld = Styles.handler(MBStyleHandler.FORMAT).parse(mbstyle, null, null, null);
        assertNotNull(sld);

        LineSymbolizer ls = SLD.lineSymbolizer(Styles.style(sld));
        assertNotNull(ls);
    }
    
    /**
     * Verify that the {@link MBStyleHandler} does not take precedence for the "json" extension in the {@link MediaTypes} registry.
     */
    @Test
    public void testForFileExtensionCollision() {
        MediaType mt = MediaTypes.getMediaTypeForExtension("json");
        assertEquals(mt, new MediaType("application/json"));
    }
}
