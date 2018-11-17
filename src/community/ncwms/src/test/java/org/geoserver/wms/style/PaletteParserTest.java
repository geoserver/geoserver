/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.style;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.awt.Color;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import javax.xml.transform.TransformerException;
import org.geotools.filter.function.EnvFunction;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.styling.SLDTransformer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;

public class PaletteParserTest {
    @Rule public ExpectedException exception = ExpectedException.none();

    PaletteParser parser = new PaletteParser();

    @Before
    public void resetEnvFunction() {
        EnvFunction.clearLocalValues();
        EnvFunction.clearGlobalValues();
    }

    @Test
    public void testParseBlackAndWhiteHash() throws IOException {
        assertBlackAndWhite("#000000\n#FFFFFF");
    }

    @Test
    public void testParseBlackAndWhiteHashAlpha() throws IOException {
        assertBlackAndWhite("#FF000000\n#FFFFFFFF");
    }

    @Test
    public void testParseBlackAndWhiteSimpleHex() throws IOException {
        assertBlackAndWhite("0x000000\n0xFFFFFF");
    }

    @Test
    public void testParseBlackAndWhiteHexAlpha() throws IOException {
        assertBlackAndWhite("0xFF000000\n0xFFFFFFFF");
    }

    @Test
    public void assertBlackAndWhiteTranslucent() throws IOException {
        assertBlackAndWhiteTranslucent("#64000000\n#64FFFFFF");
    }

    @Test
    public void assertBlackAndWhiteHexTranslucent() throws IOException {
        assertBlackAndWhiteTranslucent("0x64000000\n0x64FFFFFF");
    }

    @Test
    public void testParseBlackAndWhiteSimpleHexComments() throws IOException {
        assertBlackAndWhite("%one\n0x000000\n%two\n0xFFFFFF\n%three\n%four");
    }

    @Test
    public void testErrorMessage() throws IOException {
        // we expect to get a error message pointing at the invalid color
        exception.expect(PaletteParser.InvalidColorException.class);
        exception.expectMessage(
                "Invalid color 'abcde', supported syntaxes are #RRGGBB, 0xRRGGBB, #AARRGGBB and 0xAARRGGBB");
        parser.parseColorMap(toReader("#FF0000\nabcde\n#000000"));
    }

    @Test
    public void testParseBlackWhiteToStyle() throws IOException, TransformerException {
        StyledLayerDescriptor sld = parser.parseStyle(toReader("#000000\n#FFFFFF"));
        Function cm = assertDynamicColorColormap(sld);
        assertEquals("rgb(0,0,0);rgb(255,255,255)", cm.getParameters().get(0).evaluate(null));
    }

    @Test
    public void testParseBlackWhiteTranslucentToStyle() throws IOException, TransformerException {
        StyledLayerDescriptor sld = parser.parseStyle(toReader("#64000000\n#64FFFFFF"));
        Function cm = assertDynamicColorColormap(sld);
        assertEquals(
                "rgba(0,0,0,0.39);rgba(255,255,255,0.39)",
                cm.getParameters().get(0).evaluate(null));
    }

    static Function assertDynamicColorColormap(StyledLayerDescriptor sld)
            throws TransformerException {
        // logStyle(sld);
        NamedLayer layer = (NamedLayer) sld.getStyledLayers()[0];
        Style style = layer.getStyles()[0];
        assertEquals(1, style.featureTypeStyles().size());
        FeatureTypeStyle fts = style.featureTypeStyles().get(0);
        Function dcm = (Function) fts.getTransformation();
        assertNotNull(dcm);
        assertEquals("ras:DynamicColorMap", dcm.getName());
        assertEquals(3, dcm.getParameters().size());
        // first param, the full data set
        assetIsParameterFunction(dcm.getParameters().get(0), "data");
        // second param, the opacity
        EnvFunction.clearLocalValues();
        Expression opacity = assetIsParameterFunction(dcm.getParameters().get(1), "opacity");
        assertEquals(1f, opacity.evaluate(null, Float.class), 0f);
        EnvFunction.setLocalValue(PaletteParser.OPACITY, "0.5");
        assertEquals(0.5f, opacity.evaluate(null, Float.class), 0f);
        // second one, the colormap
        Expression crValue = assetIsParameterFunction(dcm.getParameters().get(2), "colorRamp");
        Function cm = (Function) crValue;
        assertEquals("colormap", cm.getName());
        return cm;
    }

    void logStyle(StyledLayerDescriptor sld) throws TransformerException {
        final SLDTransformer tx = new SLDTransformer();
        tx.setIndentation(2);
        tx.transform(sld, System.out);
    }

    private static Expression assetIsParameterFunction(Expression e, String key) {
        Function p1 = (Function) e;
        assertEquals("parameter", p1.getName());
        assertEquals(key, p1.getParameters().get(0).evaluate(null));
        if ("data".equals(key)) {
            assertEquals(1, p1.getParameters().size());
            return null;
        } else {
            assertEquals(2, p1.getParameters().size());
            return p1.getParameters().get(1);
        }
    }

    private void assertBlackAndWhite(String palette) throws IOException {
        List<Color> colors = parser.parseColorMap(toReader(palette));
        assertEquals(2, colors.size());
        assertEquals(Color.BLACK, colors.get(0));
        assertEquals(Color.WHITE, colors.get(1));
    }

    private void assertBlackAndWhiteTranslucent(String palette) throws IOException {
        List<Color> colors = parser.parseColorMap(toReader(palette));
        assertEquals(2, colors.size());
        assertEquals(new Color(0, 0, 0, 100), colors.get(0));
        assertEquals(new Color(255, 255, 255, 100), colors.get(1));
    }

    private Reader toReader(String palette) {
        return new StringReader(palette);
    }
}
