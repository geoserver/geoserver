/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2025, Open Source Geospatial Foundation (OSGeo)
 *
 *    This file is hereby placed into the Public Domain. This means anyone is
 *    free to do whatever they wish with this file. Use it well and enjoy!
 */
package org.geotools.dggs.clickhouse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.data.DataUtilities;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.NearestVisitor;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCDateOnlineTest;
import org.geotools.jdbc.JDBCTestSetup;
import org.geotools.jdbc.JDBCTestSupport;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

/**
 * Fork of {@link JDBCDateOnlineTest} for ClickHouse, which has no support for Time columns, and might never have one
 * (support for it has been marked as won't fix: https://github.com/ClickHouse/ClickHouse/issues/979).
 */
public class ClickHouseDateOnlineTest extends JDBCTestSupport {

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new ClickHouseDateTestSetup();
    }

    @Override
    protected Map<String, Object> createDataStoreFactoryParams() throws Exception {
        Map<String, Object> params = super.createDataStoreFactoryParams();
        params.put(JDBCDataStoreFactory.SCHEMA.key, params.get(JDBCDataStoreFactory.DATABASE.key));
        return params;
    }

    @Test
    public void testReadData() throws Exception {
        SimpleFeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        SimpleFeature f = DataUtilities.first(fs.getFeatures());
        // just checking that we can read the data without class cast exceptions
        Date date = (Date) f.getAttribute(aname("d"));
        assertNotNull(date);
        Timestamp timestamp = (Timestamp) f.getAttribute(aname("dt"));
        assertNotNull(timestamp);
    }

    @Test
    public void testMappings() throws Exception {
        SimpleFeatureType ft = dataStore.getSchema(tname("dates"));

        assertEquals(java.sql.Date.class, ft.getDescriptor(aname("d")).getType().getBinding());
        assertEquals(Timestamp.class, ft.getDescriptor(aname("dt")).getType().getBinding());
    }

    @Test
    public void testFiltersByDate() throws Exception {
        FeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        FilterFactory ff = dataStore.getFilterFactory();
        Filter f = ff.lessOrEqual(ff.property(aname("d")), ff.literal(df.parse("2009-06-28")));
        assertEquals(2, fs.getCount(new Query(tname("dates"), f)));
    }

    @Test
    public void testFilterByTimeStamp() throws Exception {
        FeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        FilterFactory ff = dataStore.getFilterFactory();

        DateFormat df = new SimpleDateFormat("yyyy-dd-MM");
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone[] zones = {
            TimeZone.getTimeZone("Etc/GMT+12"),
            TimeZone.getTimeZone("America/Los_Angeles"), // PST
            TimeZone.getTimeZone("America/New_York"), // EST
            TimeZone.getTimeZone("Etc/GMT"), // GMT
            TimeZone.getTimeZone("Europe/Berlin"), // CET (commonly represents Central European Time)
            TimeZone.getTimeZone("Etc/GMT-12"),
            TimeZone.getTimeZone("Etc/GMT-14")
        };

        try {
            for (TimeZone zone : zones) {
                // set JVM time zone
                TimeZone.setDefault(zone);

                // regenerate the database table using the new JVM Timezone
                ((ClickHouseDateTestSetup) setup).setUpData();
                df.setTimeZone(zone);

                Filter f = ff.equals(ff.property(aname("dt")), ff.literal("2009-06-28 15:12:41"));
                assertEquals(1, fs.getCount(new Query(tname("dates"), f)));
            }

        } finally {
            // set JVM time zone
            TimeZone.setDefault(originalTimeZone);
            // regenerate the database table using the new JVM Timezone
            ((ClickHouseDateTestSetup) setup).setUpData();
        }
    }

    @Test
    public void testMinDate() throws Exception {
        SimpleFeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        MinVisitor visitor = new MinVisitor(aname("d"));
        fs.getFeatures().accepts(visitor, null);
        Date date = (Date) visitor.getMin();
        assertNotNull(date);
    }

    @Test
    public void testMaxDate() throws Exception {
        SimpleFeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        MaxVisitor visitor = new MaxVisitor(aname("d"));
        fs.getFeatures().accepts(visitor, null);
        Date date = (Date) visitor.getMax();
        assertNotNull(date);
    }

    @Test
    public void testDistinctDate() throws Exception {
        SimpleFeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        UniqueVisitor visitor = new UniqueVisitor(aname("d"));
        fs.getFeatures().accepts(visitor, null);
        Set results = visitor.getUnique();
        for (Object result : results) {
            assertThat(result, CoreMatchers.instanceOf(Date.class));
        }
    }

    @Test
    public void testNearestDate() throws Exception {
        SimpleFeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        FilterFactory ff = dataStore.getFilterFactory();
        NearestVisitor visitor = new NearestVisitor(ff.property(aname("d")), new Date());
        fs.getFeatures().accepts(visitor, null);
        Date date = (Date) visitor.getNearestMatch();
        assertNotNull(date);
    }

    @Test
    public void testMinTimeStamp() throws Exception {
        SimpleFeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        MinVisitor visitor = new MinVisitor(aname("dt"));
        fs.getFeatures().accepts(visitor, null);
        Timestamp ts = (Timestamp) visitor.getMin();
        assertNotNull(ts);
    }

    @Test
    public void testMaxTimeStamp() throws Exception {
        SimpleFeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        MaxVisitor visitor = new MaxVisitor(aname("dt"));
        fs.getFeatures().accepts(visitor, null);
        Timestamp ts = (Timestamp) visitor.getMax();
        assertNotNull(ts);
    }

    @Test
    public void testDistinctTimeStamp() throws Exception {
        SimpleFeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        UniqueVisitor visitor = new UniqueVisitor(aname("dt"));
        fs.getFeatures().accepts(visitor, null);
        Set results = visitor.getUnique();
        for (Object result : results) {
            assertThat(result, CoreMatchers.instanceOf(Timestamp.class));
        }
    }

    @Test
    public void testNearestTimeStamp() throws Exception {
        SimpleFeatureSource fs = dataStore.getFeatureSource(tname("dates"));
        FilterFactory ff = dataStore.getFilterFactory();
        NearestVisitor visitor =
                new NearestVisitor(ff.property(aname("dt")), new Timestamp(System.currentTimeMillis()));
        fs.getFeatures().accepts(visitor, null);
        Timestamp ts = (Timestamp) visitor.getNearestMatch();
        assertNotNull(ts);
    }
}
