/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata.jdbc.utils;

/**
 * Utilities for getting parameters information from a JDBC URL
 *
 * @author Jose Macchi - Geosolutions
 */
public class JdbcUrlSplitter {
    public String driverName, host, port, database, params;

    public JdbcUrlSplitter(String jdbcUrl) {
        int pos, pos1, pos2;
        String connUri;

        if (jdbcUrl == null
                || !jdbcUrl.startsWith("jdbc:")
                || (pos1 = jdbcUrl.indexOf(':', 5)) == -1)
            throw new IllegalArgumentException("Invalid JDBC url.");

        driverName = jdbcUrl.substring(5, pos1);
        if ((pos2 = jdbcUrl.indexOf(';', pos1)) == -1) {
            connUri = jdbcUrl.substring(pos1 + 1);
        } else {
            connUri = jdbcUrl.substring(pos1 + 1, pos2);
            params = jdbcUrl.substring(pos2 + 1);
        }

        if (connUri.startsWith("//")) {
            if ((pos = connUri.indexOf('/', 2)) != -1) {
                host = connUri.substring(2, pos);
                database = connUri.substring(pos + 1);

                if ((pos = host.indexOf(':')) != -1) {
                    port = host.substring(pos + 1);
                    host = host.substring(0, pos);
                }
            }
        } else {
            database = connUri;
        }
    }
}
