/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.vfny.geoserver.wcs.WcsException.WcsExceptionCode.InvalidParameterValue;

import java.util.List;
import net.opengis.wcs11.AxisSubsetType;
import net.opengis.wcs11.FieldSubsetType;
import net.opengis.wcs11.RangeSubsetType;
import org.junit.Test;
import org.vfny.geoserver.wcs.WcsException;

public class RangeSubsetKvpParserTest {
    RangeSubsetKvpParser parser = new RangeSubsetKvpParser();

    @Test
    public void testSimpleFields() throws Exception {
        RangeSubsetType rs = (RangeSubsetType) parser.parse("radiance;temperature");
        assertNotNull(rs);
        assertEquals(2, rs.getFieldSubset().size());
        FieldSubsetType field = (FieldSubsetType) rs.getFieldSubset().get(0);
        assertEquals("radiance", field.getIdentifier().getValue());
        assertEquals(null, field.getInterpolationType());
        field = (FieldSubsetType) rs.getFieldSubset().get(1);
        assertEquals("temperature", field.getIdentifier().getValue());
        assertEquals(null, field.getInterpolationType());
    }

    @Test
    public void testInvalidInterpolation() throws Exception {
        try {
            parser.parse("radiance:mindReadingWarper");
            fail("We do not support _that_ interpolation!");
        } catch (WcsException e) {
            assertEquals(InvalidParameterValue.toString(), e.getCode());
            assertEquals("RangeSubset", e.getLocator());
        }
    }

    @Test
    public void testInterpolation() throws Exception {
        RangeSubsetType rs = (RangeSubsetType) parser.parse("radiance:linear;temperature:nearest");
        assertNotNull(rs);
        assertEquals(2, rs.getFieldSubset().size());
        FieldSubsetType field = (FieldSubsetType) rs.getFieldSubset().get(0);
        assertEquals("radiance", field.getIdentifier().getValue());
        assertEquals("linear", field.getInterpolationType());
        field = (FieldSubsetType) rs.getFieldSubset().get(1);
        assertEquals("temperature", field.getIdentifier().getValue());
        assertEquals("nearest", field.getInterpolationType());
    }

    @Test
    public void testAxisSingleKey() throws Exception {
        RangeSubsetType rs = (RangeSubsetType) parser.parse("radiance[bands[Red]]");
        assertNotNull(rs);
        assertEquals(1, rs.getFieldSubset().size());
        FieldSubsetType field = (FieldSubsetType) rs.getFieldSubset().get(0);
        assertEquals("radiance", field.getIdentifier().getValue());
        assertEquals(1, field.getAxisSubset().size());
        AxisSubsetType axis = (AxisSubsetType) field.getAxisSubset().get(0);
        assertEquals("bands", axis.getIdentifier());
        List keys = axis.getKey();
        assertEquals(1, keys.size());
        assertEquals("Red", keys.get(0));
    }

    @Test
    public void testAxisKeys() throws Exception {
        RangeSubsetType rs = (RangeSubsetType) parser.parse("radiance[bands[Red,Green,Blue]]");
        assertNotNull(rs);
        assertEquals(1, rs.getFieldSubset().size());
        FieldSubsetType field = (FieldSubsetType) rs.getFieldSubset().get(0);
        assertEquals("radiance", field.getIdentifier().getValue());
        assertEquals(1, field.getAxisSubset().size());
        AxisSubsetType axis = (AxisSubsetType) field.getAxisSubset().get(0);
        assertEquals("bands", axis.getIdentifier());
        List keys = axis.getKey();
        assertEquals(3, keys.size());
        assertEquals("Red", keys.get(0));
        assertEquals("Green", keys.get(1));
        assertEquals("Blue", keys.get(2));
    }
}
