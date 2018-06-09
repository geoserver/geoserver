/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbc.metrics;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.geotools.jdbc.JDBCFeatureReader;
import org.geotools.jdbc.JDBCReaderCallback;

/**
 * JDBC reader callback that tracks metrics on a request by request basis.
 *
 * <p>Metrics are stored in a thread local variable. See {@link RequestMetricsFilter}.
 */
public class RequestMetricsCallback implements JDBCReaderCallback {

    static ThreadLocal<Map<String, Object>> metrics = new ThreadLocal<>();

    long start;
    long count;

    @Override
    public void init(JDBCFeatureReader reader) {
        metrics.set(new HashMap<>());
        count = 0;
    }

    @Override
    public void beforeQuery(Statement st) {
        start = System.currentTimeMillis();
        metrics.get().put("start", start);
    }

    @Override
    public void afterQuery(Statement st) {
        metrics.get().put("query", System.currentTimeMillis() - start);
    }

    @Override
    public void queryError(Exception e) {}

    @Override
    public void beforeNext(ResultSet rs) {}

    @Override
    public void afterNext(ResultSet rs, boolean hasMore) {
        if (hasMore) {
            count++;
        } else {
            metrics.get().put("total", System.currentTimeMillis() - start);
            metrics.get().put("count", count);
        }
    }

    @Override
    public void rowError(Exception e) {}

    @Override
    public void finish(JDBCFeatureReader reader) {}
}
