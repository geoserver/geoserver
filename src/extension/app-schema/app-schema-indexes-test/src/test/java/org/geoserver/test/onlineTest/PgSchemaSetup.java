/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;

/** Utility class for to insert postgresql schema data */
public class PgSchemaSetup {

    private PostgresqlProperties properties;

    public PgSchemaSetup(PostgresqlProperties properties) {
        this.properties = properties;
    }

    /** execute statements and initializes pg schema data */
    public void init() {
        try (Connection conn = getConnection()) {
            // iterate statements
            List<String> statements = getStatements();
            for (String st : statements) {
                // for every statement, execute it
                try (PreparedStatement pstmt = conn.prepareStatement(st)) {
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection() throws SQLException {
        String connUrl =
                "jdbc:postgresql://"
                        + properties.getHost()
                        + ":"
                        + properties.getPort()
                        + "/"
                        + properties.getDatabase();
        return DriverManager.getConnection(connUrl, properties.getUser(), properties.getPassword());
    }

    /**
     * get sql batch and split into statemens by ; char
     *
     * @return List of statements
     */
    protected List<String> getStatements() {
        List<String> result = new ArrayList<>();
        String sql = getSqlBatch();
        result.addAll(Arrays.asList(sql.split(";")));
        return result;
    }

    protected String getSqlBatch() {
        String sql = null;
        try {
            sql =
                    IOUtils.toString(
                            getClass()
                                    .getClassLoader()
                                    .getResourceAsStream("test-data/stations.sql"),
                            StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sql;
    }

    public PostgresqlProperties getProperties() {
        return properties;
    }
}
