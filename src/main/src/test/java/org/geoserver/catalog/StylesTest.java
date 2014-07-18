package org.geoserver.catalog;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.styling.*;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StylesTest extends GeoServerSystemTestSupport {

    @Test
    public void testLookup() throws Exception {
        assertTrue(Styles.handler(SLDHandler.FORMAT) instanceof SLDHandler);
        assertTrue(Styles.handler(PropertyStyleHandler.FORMAT) instanceof PropertyStyleHandler);
        try {
            Styles.handler(null);
            fail();
        }
        catch(Exception e) {}

        try {
            Styles.handler("foo");
            fail();
        }
        catch(Exception e) {}
    }

    @Test
    public void testParse() throws Exception {
        Properties props = new Properties();
        props.setProperty("type", "point");
        props.setProperty("color", "ff0000");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        props.store(bout, null);

        StyledLayerDescriptor sld = Styles.handler(PropertyStyleHandler.FORMAT)
            .parse(new ByteArrayInputStream(bout.toByteArray()), null, null, null);
        assertNotNull(sld);

        Style style = Styles.style(sld);
        PointSymbolizer point = SLD.pointSymbolizer(style);
        assertNotNull(point);
    }

}
