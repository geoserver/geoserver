package org.geoserver.community.mbstyle.web;

import org.geoserver.catalog.Styles;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.styling.*;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

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
}
