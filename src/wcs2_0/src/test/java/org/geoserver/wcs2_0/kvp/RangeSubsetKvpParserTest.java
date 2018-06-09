/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.kvp;

import static org.junit.Assert.*;

import net.opengis.wcs20.RangeItemType;
import net.opengis.wcs20.RangeSubsetType;
import org.eclipse.emf.common.util.EList;
import org.geoserver.platform.OWS20Exception;
import org.junit.Test;

public class RangeSubsetKvpParserTest {

    RangeSubsetKvpParser parser = new RangeSubsetKvpParser();

    @Test
    public void testInvalidValues() throws Exception {
        try {
            parser.parse("axis::blah");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("band1,band2:band3:band4");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("band1,,band2");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }

        try {
            parser.parse("band1,band2,");
            fail("should have thrown an exception");
        } catch (OWS20Exception e) {
            checkInvalidSyntaxException(e);
        }
    }

    private void checkInvalidSyntaxException(OWS20Exception e) {
        assertNotNull(e.getHttpCode());
        assertEquals(400, e.getHttpCode().intValue());
        assertEquals("InvalidEncodingSyntax", e.getCode());
        assertEquals("rangeSubset", e.getLocator());
    }

    @Test
    public void testMixed() throws Exception {
        RangeSubsetType rs =
                (RangeSubsetType) parser.parse("band01,band03:band05,band10,band19:band21");
        EList<RangeItemType> items = rs.getRangeItems();
        assertEquals(4, items.size());
        RangeItemType i1 = items.get(0);
        assertEquals("band01", i1.getRangeComponent());
        RangeItemType i2 = items.get(1);
        assertEquals("band03", i2.getRangeInterval().getStartComponent());
        assertEquals("band05", i2.getRangeInterval().getEndComponent());
        RangeItemType i3 = items.get(2);
        assertEquals("band10", i3.getRangeComponent());
        RangeItemType i4 = items.get(3);
        assertEquals("band19", i4.getRangeInterval().getStartComponent());
        assertEquals("band21", i4.getRangeInterval().getEndComponent());
    }
}
