package org.geoserver.gss.impl;

import java.util.Date;

import junit.framework.TestCase;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.util.Range;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

public class TimeConstraintExtractorTest extends TestCase {

    private static final String UPDATED_PROPERTY = "atom:entry/atom:updated";

    private static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
            .getDefaultHints());

    private TimeConstraintExtractor timeConstraintExtractor;

    @Override
    protected void setUp() throws Exception {
        timeConstraintExtractor = new TimeConstraintExtractor();
    }

    public void testPropertyIsEqualTo() {
        Filter filter = ff.equals(ff.property(UPDATED_PROPERTY), ff.literal(new Date(1000)));

        @SuppressWarnings("unchecked")
        Range<Date> range = (Range<Date>) filter.accept(timeConstraintExtractor, null);
        assertNotNull(range);

        assertEquals(new Date(1000), range.getMinValue());
        assertTrue(range.isMinIncluded());

        assertEquals(new Date(1000), range.getMaxValue());
        assertTrue(range.isMaxIncluded());
    }

    public void testPropertyIsGreaterThan() {
        Filter filter = ff.greater(ff.property(UPDATED_PROPERTY), ff.literal(new Date(1000)));

        @SuppressWarnings("unchecked")
        Range<Date> range = (Range<Date>) filter.accept(timeConstraintExtractor, null);
        assertNotNull(range);

        assertEquals(new Date(1000), range.getMinValue());
        assertFalse(range.isMinIncluded());

        assertEquals(new Date(Long.MAX_VALUE), range.getMaxValue());
        assertTrue(range.isMaxIncluded());
    }

    public void testPropertyIsLessThan() {
        Filter filter = ff.less(ff.property(UPDATED_PROPERTY), ff.literal(new Date(1000)));

        @SuppressWarnings("unchecked")
        Range<Date> range = (Range<Date>) filter.accept(timeConstraintExtractor, null);
        assertNotNull(range);

        assertEquals(new Date(0), range.getMinValue());
        assertTrue(range.isMinIncluded());

        assertEquals(new Date(1000), range.getMaxValue());
        assertFalse(range.isMaxIncluded());
    }

    public void testPropertyIsLessThanOrEqualTo() {
        Filter filter = ff.lessOrEqual(ff.property(UPDATED_PROPERTY), ff.literal(new Date(1000)));

        @SuppressWarnings("unchecked")
        Range<Date> range = (Range<Date>) filter.accept(timeConstraintExtractor, null);
        assertNotNull(range);

        assertEquals(new Date(0), range.getMinValue());
        assertTrue(range.isMinIncluded());

        assertEquals(new Date(1000), range.getMaxValue());
        assertTrue(range.isMaxIncluded());
    }

    public void testPropertyIsGreaterThanOrEqualTo() {
        Filter filter = ff
                .greaterOrEqual(ff.property(UPDATED_PROPERTY), ff.literal(new Date(1000)));

        @SuppressWarnings("unchecked")
        Range<Date> range = (Range<Date>) filter.accept(timeConstraintExtractor, null);
        assertNotNull(range);

        assertEquals(new Date(1000), range.getMinValue());
        assertTrue(range.isMinIncluded());

        assertEquals(new Date(Long.MAX_VALUE), range.getMaxValue());
        assertTrue(range.isMaxIncluded());
    }

    public void testPropertyIsBetween() {
        Filter filter = ff.between(ff.property(UPDATED_PROPERTY), ff.literal(new Date(1000)),
                ff.literal(new Date(2000)));

        @SuppressWarnings("unchecked")
        Range<Date> range = (Range<Date>) filter.accept(timeConstraintExtractor, null);
        assertNotNull(range);

        assertEquals(new Date(1000), range.getMinValue());
        assertFalse(range.isMinIncluded());

        assertEquals(new Date(2000), range.getMaxValue());
        assertFalse(range.isMaxIncluded());
    }
}
