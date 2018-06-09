/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbc.metrics;

import org.geotools.jdbc.JDBCCallbackFactory;

/** Factory for the request metrics callback. */
public class RequestMetricsFactory implements JDBCCallbackFactory {

    @Override
    public String getName() {
        return "request-metrics";
    }

    @Override
    public RequestMetricsCallback createReaderCallback() {
        return new RequestMetricsCallback();
    }
}
