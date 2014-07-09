package org.geoserver.community.css.web;

import org.geoserver.catalog.Styles;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.styling.*;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class CssStyleHandlerTest extends GeoServerSystemTestSupport {

    @Test
    public void testParseThroughStyles() throws IOException {
        String css = "* { fill: lightgrey; }";
        StyledLayerDescriptor sld = Styles.parse(css, CssStyleHandler.FORMAT);
        assertNotNull(sld);

        PolygonSymbolizer ps = SLD.polySymbolizer(Styles.style(sld));
        assertNotNull(ps);
    }
}
