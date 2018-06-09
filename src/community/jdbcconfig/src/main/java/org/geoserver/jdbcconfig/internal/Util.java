/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

public class Util {

    /**
     * Reads SQL from the specified script and executes against a JdbcOperations instance.
     *
     * <p>A few notes about the format of the file:
     *
     * <ul>
     *   <li>Statements may span multiple lines, and must be terminated with a ";"
     *   <li>Lines starting with "--" are considered comments and ignored
     *   <li>Statements may be preceded with "?" to signal that it is ok if the statement fails
     * </ul>
     */
    public static void runScript(InputStream script, JdbcOperations jdbc, Logger logger)
            throws IOException {
        List<String> lines = org.apache.commons.io.IOUtils.readLines(script);

        StringBuilder buf = new StringBuilder();
        for (String sql : lines) {
            sql = sql.trim();
            if (sql.isEmpty()) {
                continue;
            }
            if (sql.startsWith("--")) {
                continue;
            }
            buf.append(sql).append(" ");
            if (sql.endsWith(";")) {
                // oracle hates semi-colons here, just use as a separator
                // for knowing when to execute a stmt, but don't include
                buf.setLength(buf.length() - 2);
                String stmt = buf.toString();
                boolean skipError = stmt.startsWith("?");
                if (skipError) {
                    stmt = stmt.replaceAll("^\\? *", "");
                }

                if (logger != null) logger.info("Running: " + stmt);

                try {
                    jdbc.update(stmt);
                } catch (DataAccessException e) {
                    if (logger != null) {
                        logger.warning(e.getMessage());
                    }
                    if (!skipError) {
                        throw e;
                    }
                }

                buf.setLength(0);
            }
        }
    }
}
