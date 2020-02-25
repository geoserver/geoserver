/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import com.google.common.base.Joiner;
import java.sql.Connection;
import java.sql.SQLException;
import javax.annotation.Nullable;
import javax.sql.DataSource;

public class Dialect {

    public static Dialect detect(DataSource dataSource) {
        Dialect dialect;
        try {
            Connection conn = dataSource.getConnection();
            String driver = conn.getMetaData().getDriverName();
            if (driver.contains("Oracle")) {
                dialect = new OracleDialect();
            } else {
                dialect = new Dialect();
            }
            conn.close();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return dialect;
    }

    public void applyOffsetLimit(
            StringBuilder sql, @Nullable Integer offset, @Nullable Integer limit) {
        // some db's require limit to be present of offset is
        if (offset != null && limit == null) {
            limit = Integer.MAX_VALUE;
        }
        if (limit != null) {
            sql.append(" limit ").append(limit);
        }
        if (offset != null) {
            sql.append(" offset ").append(offset);
        }
    }

    public String nextVal(String sequence) {
        return "DEFAULT";
    }

    public CharSequence propertyName(String propertyName) {
        return Joiner.on("").join(identifierQualifier(), propertyName, identifierQualifier());
    }

    private String identifierQualifier() {
        return "";
    }

    public CharSequence iLikeArgument(CharSequence subsequence) {
        return Joiner.on("").join("%", String.valueOf(subsequence).toLowerCase(), "%");
    }

    public CharSequence iLikeNamedPreparedConstruct(String attributeName, String valueParam) {
        return Joiner.on("").join("lower(", propertyName(attributeName), ") like :", valueParam);
    }
}
