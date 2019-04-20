/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore;

import static org.easymock.EasyMock.*;

import com.google.common.base.Optional;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.jdbcstore.internal.JDBCResourceStoreProperties;
import org.geoserver.platform.resource.URIs;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
public class PostgresTestSupport implements DatabaseTestSupport {

    JDBCResourceStore store;
    PGSimpleDataSource ds;
    Connection conn;
    PreparedStatement insert;

    public PostgresTestSupport() throws Exception {
        ds = createTestDataSource();
        conn = ds.getConnection();
        try {
            insert =
                    conn.prepareStatement(
                            "INSERT INTO resources (name, parent, content) VALUES (?, ?, ?) RETURNING oid;");
        } finally {
            if (insert == null) conn.close();
        }
    }

    private static PGSimpleDataSource createTestDataSource() throws SQLException {
        PGSimpleDataSource ds = new PGSimpleDataSource();

        ds.setServerName("localhost");
        ds.setDatabaseName("jdbcstoretest");
        ds.setPortNumber(5432);
        ds.setUser("jdbcstore");
        ds.setPassword("jdbcstore");

        // Ensure the database is empty
        try (Connection conn = ds.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP SCHEMA IF EXISTS public CASCADE;");
                stmt.execute("CREATE SCHEMA public;");
                stmt.execute("GRANT ALL ON SCHEMA public TO postgres;");
                stmt.execute("GRANT ALL ON SCHEMA public TO public;");
                stmt.execute("COMMENT ON SCHEMA public IS 'standard public schema';");
            }
        }

        return ds;
    }

    @Override
    public void stubConfig(JDBCResourceStoreProperties config) {
        expect(config.getInitScript())
                .andStubReturn(
                        URIs.asResource(
                                JDBCResourceStoreProperties.class.getResource(
                                        "init.postgres.sql")));
        expect(config.getJdbcUrl())
                .andStubReturn(Optional.of("jdbc:postgresql://localhost:5432/jdbcstoretest"));
        expect(config.getJndiName()).andStubReturn(Optional.<String>absent());
        expect(config.getProperty(eq("username"))).andStubReturn("jdbcstore");
        expect(config.getProperty(eq("username"), (String) anyObject())).andStubReturn("jdbcstore");
        expect(config.getProperty(eq("password"))).andStubReturn("jdbcstore");
        expect(config.getProperty(eq("password"), (String) anyObject())).andStubReturn("jdbcstore");
        expect(config.getProperty(eq("driverClassName"))).andStubReturn("org.postgresql.Driver");
        expect(config.getProperty(eq("driverClassName"), (String) anyObject()))
                .andStubReturn("org.postgresql.Driver");
    }

    @Override
    public void initialize() throws Exception {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);

        try (InputStream in =
                JDBCResourceStoreProperties.class.getResourceAsStream("init.postgres.sql")) {
            Util.runScript(in, template.getJdbcOperations(), null);
        }
    }

    @Override
    public int addFile(String name, int parent, byte[] content) throws SQLException {
        insert.setString(1, name);
        insert.setInt(2, parent);
        insert.setBytes(3, content);
        ResultSet rs = insert.executeQuery();
        if (rs.next()) {
            return rs.getInt("oid");
        } else {
            throw new IllegalStateException("Could not add test file " + name);
        }
    }

    @Override
    public int addDir(String name, int parent) throws SQLException {
        insert.setString(1, name);
        insert.setInt(2, parent);
        insert.setBytes(3, null);
        ResultSet rs = insert.executeQuery();
        if (rs.next()) {
            return rs.getInt("oid");
        } else {
            throw new IllegalStateException("Could not add test directory " + name);
        }
    }

    @Override
    public int getRoot() {
        return 0;
    }

    @Override
    public DataSource getDataSource() {
        return ds;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return conn;
    }

    @Override
    public void close() throws SQLException {
        conn.close();
    }
}
