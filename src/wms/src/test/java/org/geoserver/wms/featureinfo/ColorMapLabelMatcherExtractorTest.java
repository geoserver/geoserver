package org.geoserver.wms.featureinfo;

import static org.geoserver.wms.featureinfo.ColorMapLabelMatcherTest.readSLD;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import org.geotools.styling.Style;
import org.junit.Test;

public class ColorMapLabelMatcherExtractorTest {

    @Test
    public void testColorMapLabelMatcherExtractor() throws IOException {
        Style style = readSLD("labelInFeatureInfoTazDem.sld", getClass());
        ColorMapLabelMatcherExtractor extractor = new ColorMapLabelMatcherExtractor(1091957.546931);
        style.accept(extractor);
        assertEquals(1, extractor.getColorMapLabelMatcherList().size());
        ColorMapLabelMatcher lifi = extractor.getColorMapLabelMatcherList().get(0);
        assertEquals("ADD", lifi.getLabelInclusion());
        assertEquals("Label", lifi.getAttributeName());
    }

    @Test
    public void testColorMapLabelMatcherExtractorCustomAttributeName() throws IOException {
        Style style = readSLD("labelCustomNameTazDem.sld", getClass());
        ColorMapLabelMatcherExtractor extractor = new ColorMapLabelMatcherExtractor(1091957.546931);
        style.accept(extractor);
        assertEquals(1, extractor.getColorMapLabelMatcherList().size());
        ColorMapLabelMatcher lifi = extractor.getColorMapLabelMatcherList().get(0);
        assertEquals("ADD", lifi.getLabelInclusion());
        assertEquals("custom name", lifi.getAttributeName());
    }
}
