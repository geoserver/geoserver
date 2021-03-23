package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.xml.styling.SLDParser;
import org.junit.Test;

public class ColorMapLabelMatcherTest {

    @Test
    public void testLabelMatchWithColorMapRamp() throws IOException {
        Style style = readSLD("labelInFeatureInfoTazDem.sld", getClass());
        ColorMapLabelMatcherExtractor extractor = new ColorMapLabelMatcherExtractor(1091957.546931);
        style.accept(extractor);
        ColorMapLabelMatcher lifi = extractor.getColorMapLabelMatcherList().get(0);
        String label = lifi.getLabelForPixel(13d);
        assertEquals("1", label);
        label = lifi.getLabelForPixel(155.98766);
        assertEquals("124.811736", label);
        label = lifi.getLabelForPixel(547.9222);
        assertEquals("559.204195", label);
        label = lifi.getLabelForPixel(46421.765);
        assertEquals("55537", label);
        label = lifi.getLabelForPixel(60000);
        assertEquals("55537", label);
    }

    @Test
    public void testLabelMatchWithColorMapTypeIntervals() throws IOException {
        Style style = readSLD("labelInFeatureInfoTazDemReplace.sld", getClass());
        ColorMapLabelMatcherExtractor extractor = new ColorMapLabelMatcherExtractor(1091957.546931);
        style.accept(extractor);
        ColorMapLabelMatcher lifi = extractor.getColorMapLabelMatcherList().get(0);
        String label = lifi.getLabelForPixel(13d);
        assertEquals(">= 1 AND < 124.811736", label);
        label = lifi.getLabelForPixel(155.98766);
        assertEquals(">= 124.811736 AND < 308.142116", label);
        label = lifi.getLabelForPixel(547.9222);
        assertEquals(">= 308.142116 AND < 752.166285", label);
        label = lifi.getLabelForPixel(46421.765);
        assertEquals(">= 752.166285 AND <= 55537", label);
        label = lifi.getLabelForPixel(60000);
        // should be null since for type interval pixel values > then last entry are not styled
        assertNull(label);
    }

    @Test
    public void testLabelMatchColorMapTypeValues() throws IOException {
        Style style = readSLD("labelInFeatureInfoTazDemColorMapValues.sld", getClass());
        ColorMapLabelMatcherExtractor extractor = new ColorMapLabelMatcherExtractor(1091957.546931);
        style.accept(extractor);
        ColorMapLabelMatcher lifi = extractor.getColorMapLabelMatcherList().get(0);
        String label = lifi.getLabelForPixel(1);
        assertEquals("value is 1", label);
        label = lifi.getLabelForPixel(124.81173566700335);
        assertEquals("value is 124.811736", label);
        label = lifi.getLabelForPixel(559.2041949413946);
        assertEquals("value is 559.204195", label);
        label = lifi.getLabelForPixel(55537d);
        assertEquals("value is 55537", label);

        // return null if not matching exactly the entry quantity since type is value
        label = lifi.getLabelForPixel(13);
        assertNull(label);
        label = lifi.getLabelForPixel(130);
        assertNull(label);
        label = lifi.getLabelForPixel(601);
        assertNull(label);
        label = lifi.getLabelForPixel(55000);
        assertNull(label);
    }

    static Style readSLD(String sldName, Class<?> context) throws IOException {
        StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);
        SLDParser stylereader = new SLDParser(styleFactory, context.getResource(sldName));
        Style[] readStyles = stylereader.readXML();

        Style style = readStyles[0];
        return style;
    }
}
