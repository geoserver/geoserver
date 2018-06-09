/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.css.web;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import org.geoserver.catalog.Styles;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.styling.*;
import org.junit.Test;

public class CssHandlerTest extends GeoServerSystemTestSupport {

    @Test
    public void testParseThroughStyles() throws IOException {
        String css = "* { fill: lightgrey; }";
        StyledLayerDescriptor sld = Styles.handler(CssHandler.FORMAT).parse(css, null, null, null);
        assertNotNull(sld);

        PolygonSymbolizer ps = SLD.polySymbolizer(Styles.style(sld));
        assertNotNull(ps);
    }
}
