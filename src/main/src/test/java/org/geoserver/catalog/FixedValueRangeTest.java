package org.geoserver.catalog;

import static org.junit.Assert.assertTrue;

import java.util.TreeSet;
import org.junit.Test;

public class FixedValueRangeTest {

    @Test
    public void testGetFixedValueRange() throws Exception {
        String time = "2017-02-27T14:00:10Z";
        String elevation = "1.0";
        TreeSet<Object> timeresult = FixedValueRange.getFixedValueRange(time);
        assertTrue(timeresult.contains(time));
        TreeSet<Object> elevationresult = FixedValueRange.getFixedValueRange(elevation);
        assertTrue(elevationresult.contains(elevation));
    }
}
