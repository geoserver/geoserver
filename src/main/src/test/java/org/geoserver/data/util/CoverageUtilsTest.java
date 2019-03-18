/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
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
        assertEquals(Integer.valueOf(1), value);
    }

    @Test
    public void testMergeExisting() throws CQLException {
        List<GeneralParameterDescriptor> descriptors =
                new ImageMosaicFormat().getReadParameters().getDescriptor().descriptors();
        GeneralParameterValue[] oldParams = new GeneralParameterValue[1];
        oldParams[0] = ImageMosaicFormat.FILTER.createValue();
        Filter filter = CQL.toFilter("a = 6");
        GeneralParameterValue[] newParams =
                CoverageUtils.mergeParameter(
                        descriptors,
                        oldParams,
                        filter,
                        ImageMosaicFormat.FILTER.getName().getCode());
        assertEquals(1, newParams.length);
        assertEquals(filter, ((ParameterValue) newParams[0]).getValue());
    }
}
