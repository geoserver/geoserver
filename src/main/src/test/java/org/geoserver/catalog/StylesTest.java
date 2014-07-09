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
        assertTrue(
            Styles.lookupHandler(SLD10Handler.FORMAT, SLD10Handler.VERSION) instanceof SLD10Handler);
        assertTrue(
            Styles.lookupHandler(SLD11Handler.FORMAT, SLD11Handler.VERSION) instanceof SLD11Handler);
        assertTrue(
            Styles.lookupHandler(SLD10Handler.FORMAT, null) instanceof SLD10Handler);
        assertTrue(
            Styles.lookupHandler(SLD11Handler.FORMAT, null) instanceof SLD10Handler);
        assertTrue(
            Styles.lookupHandler(PropertyStyleHandler.FORMAT, null) instanceof PropertyStyleHandler);
        try {
            Styles.lookupHandler(null, null);
            fail();
        }
        catch(Exception e) {}

        try {
            Styles.lookupHandler("foo", null);
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

        StyledLayerDescriptor sld =
            Styles.parse(new ByteArrayInputStream(bout.toByteArray()), PropertyStyleHandler.FORMAT);
        assertNotNull(sld);

        Style style = Styles.style(sld);
        PointSymbolizer point = SLD.pointSymbolizer(style);
        assertNotNull(point);
    }

}
