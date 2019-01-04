/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.util;

import static org.junit.Assert.*;

import java.awt.Color;
import java.util.Collections;
import java.util.Map;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.junit.Test;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValue;

public class CoverageUtilsTest {

    @Test
    public void testGetOutputTransparentColor() {
        ParameterDescriptor<Color> pdescriptor = ImageMosaicFormat.OUTPUT_TRANSPARENT_COLOR;
        ParameterValue<Color> pvalue = pdescriptor.createValue();
        String key = pdescriptor.getName().getCode();
        Map values = Collections.singletonMap(key, "0xFFFFFF");
        Object value = CoverageUtils.getCvParamValue(key, pvalue, values);
        assertTrue(value instanceof Color);
        assertEquals(Color.WHITE, value);
    }

    @Test
    public void testMaxTiles() {
        ParameterDescriptor<Integer> pdescriptor = ImageMosaicFormat.MAX_ALLOWED_TILES;
        ParameterValue<Integer> pvalue = pdescriptor.createValue();
        String key = pdescriptor.getName().getCode();
        Map values = Collections.singletonMap(key, "1");
        Object value = CoverageUtils.getCvParamValue(key, pvalue, values);
        assertTrue(value instanceof Integer);
        assertEquals(new Integer(1), value);
    }
}
