/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;
import org.junit.Test;

public class DbUtilsTest {

    @Test
    public void testReplacingNullParameters() {
        assertEquals("foo", DbUtils.getLogStatement("foo", null));
    }

    @Test
    public void testReplacingEmptyParameters() {
        assertEquals("foo", DbUtils.getLogStatement("foo", DbUtils.params()));
    }

    @Test
    public void testReplacingOneParameter() {
        Map<String, ?> params = DbUtils.params("v0", Arrays.asList(0, 1));
        String actual = DbUtils.getLogStatement("(:v0), (:v0)", params);
        assertEquals("(0, 1), (0, 1)", actual);
    }

    @Test
    public void testReplacingTenParameters() {
        Map<String, ?> params =
                DbUtils.params(
                        "v0", null, "v1", 95, "v2", 85, "v3", 75, "v4", 65, "v5", 55, "v6", 45,
                        "v7", 35, "v8", 25, "v9", "'", "v10", "100");
        String sql = ":v0, :v1, :v1, :v2, :v3, :v4, :v5, :v6, :v7, :v8, :v9, :v10, :v10, :v10";
        String actual = DbUtils.getLogStatement(sql, params);
        String expected = "null, 95, 95, 85, 75, 65, 55, 45, 35, 25, '''', '100', '100', '100'";
        assertEquals(expected, actual);
    }
}
