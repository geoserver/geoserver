/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.dggs.clickhouse;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ClickHouseFilterToSQLTest {

    FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    private static TimeZone defaultTimeZone;

    @BeforeClass
    public static void getDefaultTimeZone() {
        defaultTimeZone = TimeZone.getDefault();
    }

    @AfterClass
    public static void resetDefaultTimeZone() {
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testDateComparisonUTC() throws Exception {
        testDateComparison(
                "UTC",
                "2023-04-01 00:00:00",
                "WHERE myDateField = parseDateTimeBestEffort('2023-04-01T00:00:00.000Z')");
    }

    @Test
    public void testDateComparisonCET() throws Exception {
        testDateComparison(
                "CET",
                "2023-04-01 00:00:00",
                "WHERE myDateField = parseDateTimeBestEffort('2023-04-01T00:00:00.000+02:00')");
    }

    private void testDateComparison(String timeZoneId, String dateString, String expectedSql) throws Exception {
        // Set up the specified time zone
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));

        // Create a date range filter
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startDate = dateFormat.parse(dateString);
        Filter filter = ff.equals(ff.property("myDateField"), ff.literal(startDate));

        // Convert the filter to SQL
        ClickHouseFilterToSQL clickHouseFilterToSQL = new ClickHouseFilterToSQL();
        String sql = clickHouseFilterToSQL.encodeToString(filter);

        // Assert that the SQL is correct for the specified time zone
        assertEquals(expectedSql, sql);
    }
}
