/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.style;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.geotools.styling.StyledLayerDescriptor;
import org.junit.Test;
import org.opengis.filter.expression.Function;

public class PaletteStyleHandlerTest {

    PaletteStyleHandler handler = new PaletteStyleHandler();

    @Test
    public void testParse() throws Exception {
        StyledLayerDescriptor sld = handler.parse("#000000\n#FFFFFF", null, null, null);
        Function cm = PaletteParserTest.assertDynamicColorColormap(sld);
        assertEquals("rgb(0,0,0);rgb(255,255,255)", cm.getParameters().get(0).evaluate(null));
    }

    @Test
    public void testValidateValid() throws Exception {
        List<Exception> exceptions = handler.validate("#000000\n#FFFFFF", null, null);
        assertEquals(0, exceptions.size());
    }

    @Test
    public void testValidateInvalid() throws Exception {
        List<Exception> exceptions = handler.validate("abcde", null, null);
        assertEquals(1, exceptions.size());
    }
}
