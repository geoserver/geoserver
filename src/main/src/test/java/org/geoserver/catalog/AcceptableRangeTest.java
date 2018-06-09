/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static junit.framework.TestCase.assertEquals;

import java.util.Date;
import org.geotools.util.DateRange;
import org.junit.Test;

public class AcceptableRangeTest {

    public static final long DAY_IN_MS = 1000 * 60 * 60 * 24;

    @Test
    public void testSymmetricTimeRange() throws Exception {
        AcceptableRange range = AcceptableRange.getAcceptableRange("P1D", Date.class);
        assertEquals(DAY_IN_MS, range.getBefore());
        assertEquals(DAY_IN_MS, range.getAfter());

        Date value = new Date();
        DateRange searchRange = (DateRange) range.getSearchRange(value);
        assertEquals(DAY_IN_MS, value.getTime() - searchRange.getMinValue().getTime());
        assertEquals(DAY_IN_MS, searchRange.getMaxValue().getTime() - value.getTime());
    }

    @Test
    public void testPastTimeRange() throws Exception {
        AcceptableRange range = AcceptableRange.getAcceptableRange("P1D/P0D", Date.class);
        assertEquals(DAY_IN_MS, range.getBefore());
        assertEquals(0l, range.getAfter());

        Date value = new Date();
        DateRange searchRange = (DateRange) range.getSearchRange(value);
        assertEquals(DAY_IN_MS, value.getTime() - searchRange.getMinValue().getTime());
        assertEquals(0l, searchRange.getMaxValue().getTime() - value.getTime());
    }

    @Test
    public void testFutureTimeRange() throws Exception {
        AcceptableRange range = AcceptableRange.getAcceptableRange("P0D/P1D", Date.class);
        assertEquals(0l, range.getBefore());
        assertEquals(DAY_IN_MS, range.getAfter());

        Date value = new Date();
        DateRange searchRange = (DateRange) range.getSearchRange(value);
        assertEquals(0l, value.getTime() - searchRange.getMinValue().getTime());
        assertEquals(DAY_IN_MS, searchRange.getMaxValue().getTime() - value.getTime());
    }
}
