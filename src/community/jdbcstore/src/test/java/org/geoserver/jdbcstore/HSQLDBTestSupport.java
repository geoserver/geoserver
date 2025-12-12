package org.geoserver.jdbcstore;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.is;

import com.google.common.base.Optional;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.geoserver.jdbcconfig.internal.Util;
import org.geoserver.jdbcstore.internal.JDBCResourceStoreProperties;
import org.geoserver.platform.resource.URIs;
import org.hsqldb.jdbc.JDBCDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
public class HSQLDBTestSupport implements DatabaseTestSupport {

    JDBCResourceStore store;
    JDBCDataSource ds;
    Connection conn;
    PreparedStatement insert;

    public HSQLDBTestSupport() throws Exception {
        ds = new JDBCDataSource();
        ds.setUrl("jdbc:hsqldb:mem:test");
        conn = ds.getConnection();
    }

    @Override
    public void stubConfig(JDBCResourceStoreProperties config) {
        expect(config.getInitScript())
                .andStubReturn(URIs.asResource(JDBCResourceStoreProperties.class.getResource("init.hsqldb.sql")));
        expect(config.getJdbcUrl()).andStubReturn(Optional.of("jdbc:hsqldb:mem:test"));
        expect(config.getJndiName()).andStubReturn(Optional.<String>absent());
        expect(config.getProperty(eq("driverClassName"))).andStubReturn("org.hsqldb.jdbc.JDBCDriver");
        expect(config.getProperty(eq("driverClassName"), (String) anyObject()))
                .andStubReturn("org.hsqldb.jdbc.JDBCDriver");
    }

    @Override
    public void initialize() throws Exception {
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(ds);

        try (InputStream in = JDBCResourceStoreProperties.class.getResourceAsStream("init.hsqldb.sql")) {
            Util.runScript(in, template.getJdbcOperations(), null);
        }
    }

    private PreparedStatement getInsert() throws SQLException {
        if (insert == null) {
            insert = conn.prepareStatement("INSERT INTO resources (name, parent, content) VALUES (?, ?, ?)");
        }
        return insert;
    }

    @Override
    public int addFile(String name, int parent, byte[] content) throws SQLException {
        getInsert().setString(1, name);
        getInsert().setInt(2, parent);
        getInsert().setBytes(3, content);
        getInsert().execute();
        ResultSet rs = getInsert().getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
        } else {
            throw new IllegalStateException("Could not add test file " + name);
        }
    }

    @Override
    public int addDir(String name, int parent) throws SQLException {
        getInsert().setString(1, name);
        getInsert().setInt(2, parent);
        getInsert().setBytes(3, null);
        getInsert().execute();
        ResultSet rs = getInsert().getGeneratedKeys();
        if (rs.next()) {
            return rs.getInt(1);
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
        conn.createStatement().execute("SHUTDOWN");
        conn.close();

        JDBCDataSource ds = new JDBCDataSource();
        ds.setUrl("jdbc:hsqldb:mem:test");
        try (Connection testConn = ds.getConnection()) {
            try (ResultSet rs = testConn.getMetaData().getTables(null, null, null, new String[] {"TABLE"})) {
                boolean result = false;
                while (rs.next()) {
                    result = true;
                }
                assertThat(result, describedAs("connection closed", is(false)));
            }
        }
    }
}
