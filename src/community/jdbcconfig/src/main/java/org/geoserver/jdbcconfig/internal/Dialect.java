/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import com.google.common.base.Joiner;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.sql.DataSource;

public class Dialect {

    // see https://github.com/hibernate/hibernate-orm/commit/59fede7acaaa1579b561407aefa582311f7ebe78
    private static final Pattern ESCAPE_CLOSING_COMMENT_PATTERN = Pattern.compile("\\*/");
    private static final Pattern ESCAPE_OPENING_COMMENT_PATTERN = Pattern.compile("/\\*");

    private boolean debugMode;

    public static Dialect detect(DataSource dataSource, boolean debugMode) {
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
        dialect.setDebugMode(debugMode);
        return dialect;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /** Escapes the contents of the SQL comment to prevent SQL injection. */
    public String escapeComment(String comment) {
        String escaped = ESCAPE_CLOSING_COMMENT_PATTERN.matcher(comment).replaceAll("*\\\\/");
        return ESCAPE_OPENING_COMMENT_PATTERN.matcher(escaped).replaceAll("/\\\\*");
    }

    /** Appends the objects to the SQL in a comment if debug mode is enabled. */
    public StringBuilder appendComment(StringBuilder sql, Object... objects) {
        if (!debugMode) {
            return sql;
        }
        sql.append(" /* ");
        for (Object object : objects) {
            sql.append(escapeComment(String.valueOf(object)));
        }
        return sql.append(" */\n");
    }

    /** Appends the objects to the SQL in an comment if debug mode is enabled. */
    public StringBuilder appendComment(Object sql, Object... objects) {
        return appendComment((StringBuilder) sql, objects);
    }

    /** Appends one of the strings to the SQL depending on whether debug mode is enabled. */
    public StringBuilder appendIfDebug(StringBuilder sql, String ifEnabled, String ifDisabled) {
        return sql.append(debugMode ? ifEnabled : ifDisabled);
    }

    public void applyOffsetLimit(
            StringBuilder sql, @Nullable Integer offset, @Nullable Integer limit) {
        // some db's require limit to be present of offset is
        if (offset != null && limit == null) {
            limit = Integer.MAX_VALUE;
        }
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }
        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
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
        return Joiner.on("").join("LOWER(", propertyName(attributeName), ") LIKE :", valueParam);
    }
}
