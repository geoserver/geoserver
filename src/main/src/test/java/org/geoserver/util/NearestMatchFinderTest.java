/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.catalog.AcceptableRange;
import org.geotools.api.feature.FeatureVisitor;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.NearestVisitor;
import org.geotools.util.DateRange;
import org.junit.Test;

/**
 * Verifies that nearest match skips the search direction an asymmetric acceptable range makes impossible (so no useless
 * query is issued), while still resolving instants, ranges and per-feature intervals correctly.
 */
public class NearestMatchFinderTest {

    private static final long HOUR = 3600 * 1000L;

    /** In memory finder, records each query filter and the visitor applied, to assert how the search is performed. */
    private static class MemoryFinder extends NearestMatchFinder {
        final SimpleFeatureType schema;
        final List<SimpleFeature> features = new ArrayList<>();
        final List<Filter> queries = new ArrayList<>();
        final List<Class<?>> visitors = new ArrayList<>();

        MemoryFinder(AcceptableRange acceptableRange, String start, String end, String typeSpec, Object[]... rows)
                throws Exception {
            super(start, end, acceptableRange, null, Date.class);
            this.schema = DataUtilities.createType("test", typeSpec);
            SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
            int id = 0;
            for (Object[] row : rows) {
                for (Object v : row) fb.add(v);
                features.add(fb.buildFeature("f" + id++));
            }
        }

        static MemoryFinder instants(AcceptableRange acceptableRange, Date... times) throws Exception {
            Object[][] rows = new Object[times.length][1];
            for (int i = 0; i < times.length; i++) rows[i][0] = times[i];
            return new MemoryFinder(acceptableRange, "time", null, "time:java.util.Date", rows);
        }

        @Override
        protected FeatureCollection getMatches(Filter filter) {
            queries.add(filter);
            List<SimpleFeature> matching =
                    features.stream().filter(filter::evaluate).collect(Collectors.toList());
            return new ListFeatureCollection(schema, matching) {
                @Override
                public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
                    visitors.add(visitor.getClass());
                    super.accepts(visitor, progress);
                }
            };
        }
    }

    private static Date date(String hhmm) {
        String[] p = hhmm.split(":");
        return new Date((Long.parseLong(p[0]) * 60 + Long.parseLong(p[1])) * 60 * 1000L);
    }

    @Test
    public void testInstantPicksClosestInSinglePass() throws Exception {
        MemoryFinder finder = MemoryFinder.instants(null, date("10:00"), date("13:30"));
        assertEquals(date("13:30"), finder.getNearest(date("13:00")));
        // a single nearest visitor finds both neighbours in one pass over the data
        assertEquals(List.of(NearestVisitor.class), finder.visitors);
    }

    @Test
    public void testInstantExactMatch() throws Exception {
        MemoryFinder finder = MemoryFinder.instants(null, date("10:00"), date("13:00"));
        assertEquals(date("13:00"), finder.getNearest(date("13:00")));
        assertEquals(List.of(NearestVisitor.class), finder.visitors);
    }

    @Test
    public void testInstantPastOnlyUsesMaxInSinglePass() throws Exception {
        AcceptableRange pastOnly = new AcceptableRange(8 * HOUR, 0L, Date.class);
        MemoryFinder finder = MemoryFinder.instants(pastOnly, date("10:00"), date("13:30"));
        // 13:30 is in the future of 13:00, not acceptable: must fall back on the past 10:00,
        // resolved with a single max visitor (no nearest, no higher search)
        assertEquals(date("10:00"), finder.getNearest(date("13:00")));
        assertEquals(List.of(MaxVisitor.class), finder.visitors);
    }

    @Test
    public void testInstantFutureOnlyUsesMinInSinglePass() throws Exception {
        AcceptableRange futureOnly = new AcceptableRange(0L, 8 * HOUR, Date.class);
        MemoryFinder finder = MemoryFinder.instants(futureOnly, date("10:00"), date("13:30"));
        // 10:00 is in the past of 13:00, not acceptable: must pick the future 13:30 with a min visitor
        assertEquals(date("13:30"), finder.getNearest(date("13:00")));
        assertEquals(List.of(MinVisitor.class), finder.visitors);
    }

    @Test
    public void testPastOnlyRangeSkipsHigherSearch() throws Exception {
        // accept up to 8h in the past, nothing in the future
        AcceptableRange pastOnly = new AcceptableRange(8 * HOUR, 0L, Date.class);
        MemoryFinder finder = MemoryFinder.instants(pastOnly, date("10:00"), date("13:30"));

        // a range value forces the lower/higher (interval) handling
        Object nearest = finder.getNearest(new DateRange(date("12:00"), date("13:00")));

        assertEquals(date("10:00"), nearest);
        assertEquals("higher search must be skipped, only one query expected", 1, finder.queries.size());
    }

    @Test
    public void testFutureOnlyRangeSkipsLowerSearch() throws Exception {
        AcceptableRange futureOnly = new AcceptableRange(0L, 8 * HOUR, Date.class);
        MemoryFinder finder = MemoryFinder.instants(futureOnly, date("10:00"), date("13:30"));

        Object nearest = finder.getNearest(new DateRange(date("12:00"), date("13:00")));

        assertEquals(date("13:30"), nearest);
        assertEquals("lower search must be skipped, only one query expected", 1, finder.queries.size());
    }

    @Test
    public void testSymmetricRangeSearchesBothSides() throws Exception {
        AcceptableRange both = new AcceptableRange(8 * HOUR, 8 * HOUR, Date.class);
        MemoryFinder finder = MemoryFinder.instants(both, date("10:00"), date("13:30"));

        // 13:30 is closer to the [12:00,13:00] range than 10:00
        Object nearest = finder.getNearest(new DateRange(date("12:00"), date("13:00")));

        assertEquals(date("13:30"), nearest);
        assertEquals("both sides must be searched", 2, finder.queries.size());
    }

    @Test
    public void testPastOnlyRangeWithNoLowerCandidate() throws Exception {
        // only data in the future, past only range: no match, no query on the higher side, no failure
        AcceptableRange pastOnly = new AcceptableRange(8 * HOUR, 0L, Date.class);
        MemoryFinder finder = MemoryFinder.instants(pastOnly, date("13:30"));

        Object nearest = finder.getNearest(new DateRange(date("12:00"), date("13:00")));

        assertNull(nearest);
        assertEquals(1, finder.queries.size());
    }

    @Test
    public void testIntervalFeaturesUseStartAndEndAttributes() throws Exception {
        // features carry a [start, end] validity interval; lowers search the end, highers the start
        MemoryFinder finder = new MemoryFinder(
                null,
                "start",
                "end",
                "start:java.util.Date,end:java.util.Date",
                new Object[] {date("08:00"), date("10:00")},
                new Object[] {date("14:00"), date("16:00")});

        // 11:00 is closer to the interval ending at 10:00 than the one starting at 14:00
        assertEquals(date("10:00"), finder.getNearest(date("11:00")));
        // 13:00 is closer to the interval starting at 14:00
        finder.queries.clear();
        assertEquals(date("14:00"), finder.getNearest(date("13:00")));
    }
}
