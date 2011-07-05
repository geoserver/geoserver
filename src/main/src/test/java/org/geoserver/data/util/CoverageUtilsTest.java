package org.geoserver.data.util;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;

import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;

public class CoverageUtilsTest extends TestCase {

    public void testGetOutputTransparentColor() {
        ParameterDescriptor<Color> pdescriptor = ImageMosaicFormat.OUTPUT_TRANSPARENT_COLOR;
        ParameterValue<Color> pvalue = pdescriptor.createValue();
        String key = pdescriptor.getName().getCode();
        Map values = Collections.singletonMap(key, "0xFFFFFF");
        Object value = CoverageUtils.getCvParamValue(key, pvalue, values);
        assertTrue(value instanceof Color);
        assertEquals(Color.WHITE, value);
    }
}
