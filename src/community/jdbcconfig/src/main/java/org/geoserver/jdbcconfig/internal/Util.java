/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

public class Util {

    /**
     * Reads SQL from the specified script and executes against a JdbcOperations instance.
     * <p>
     * A few notes about the format of the file:
     * <ul>
     *   <li>Statements may span multiple lines, and must be terminated with a ";"
     *   <li>Lines starting with "--" are considered comments and ignored
     *   <li>Statements may be preceded with "?" to signal that it is ok if the statement fails  
     * </ul>
     * </p>
     */
    public static void runScript(URL script, JdbcOperations jdbc, Logger logger) throws IOException {
        InputStream stream = script.openStream();
        List<String> lines;
        try {
            lines = org.apache.commons.io.IOUtils.readLines(stream);
        } finally {
            stream.close();
        }

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
                String stmt = buf.toString();
                boolean skipError = stmt.startsWith("?");
                if (skipError) {
                    stmt = stmt.replaceAll("^\\? *" ,"");
                }

                if (logger != null) logger.info("Running: " + stmt);

                try {
                    jdbc.update(stmt);
                }
                catch(DataAccessException e) {
                    if (!skipError) {
                        throw e;
                    }
                }

                buf.setLength(0);
            }
        }
    }
}
