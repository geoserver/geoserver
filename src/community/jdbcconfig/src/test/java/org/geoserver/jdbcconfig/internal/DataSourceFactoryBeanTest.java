/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import com.google.common.base.Optional;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSource;
import org.easymock.EasyMock;
import org.geoserver.jdbcloader.DataSourceFactoryBean;
import org.junit.Test;

/** @author Kevin Smith, OpenGeo */
public class DataSourceFactoryBeanTest {

    @Test
    public void testBasic() throws Exception {
        final BasicDataSource ds = EasyMock.createMock(BasicDataSource.class);
        JDBCConfigProperties config = EasyMock.createMock(JDBCConfigProperties.class);
        Context jndi = EasyMock.createMock(Context.class);

        expect(config.isEnabled()).andReturn(true);
        expectJndi(config, null);

        expect(config.getJdbcUrl()).andStubReturn(Optional.of("jdbc:test"));
        ds.setUrl("jdbc:test");
        expectLastCall();
        expect(config.getProperty("driverClassName"))
                .andStubReturn("org.geoserver.jdbcconfig.internal.MockJDBCDriver");
        ds.setDriverClassName("org.geoserver.jdbcconfig.internal.MockJDBCDriver");
        expectLastCall();
        expect(config.getProperty("username")).andStubReturn("testUser");
        ds.setUsername("testUser");
        expect(config.getProperty("password")).andStubReturn("swordfish");
        ds.setPassword("swordfish");

        expect(config.getProperty("pool.minIdle")).andStubReturn(null);
        expect(config.getProperty("pool.maxActive")).andStubReturn(null);
        expect(config.getProperty("pool.poolPreparedStatements")).andStubReturn(null);
        expect(config.getProperty("pool.maxOpenPreparedStatements")).andStubReturn(null);
        expect(config.getProperty("pool.testOnBorrow")).andStubReturn(null);

        config.setDatasourceId("jdbc:test");
        expectLastCall();

        ds.setMinIdle(1);
        expectLastCall();
        ds.setMaxActive(10);
        expectLastCall();
        ds.setPoolPreparedStatements(true);
        expectLastCall();
        ds.setMaxOpenPreparedStatements(50);
        expectLastCall();

        expectVerifyConnect(ds);
        replay(ds, config, jndi);

        DataSourceFactoryBean fact =
                new DataSourceFactoryBean(config, jndi) {

                    @Override
                    protected BasicDataSource createBasicDataSource() {
                        return ds;
                    }
                };

        // Check that we get the DataSource
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds);
        reset(ds);
        replay(ds);

        // Check that the same DataSource is returned on subsequent calls without any changes
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds, config, jndi);

        // Check that destruction properly closes the DataSource
        reset(ds);
        ds.close();
        expectLastCall();
        replay(ds);
        fact.destroy();
        verify(ds);
    }

    @Test
    public void testJNDI() throws Exception {
        DataSource ds = EasyMock.createMock(DataSource.class);
        JDBCConfigProperties config = EasyMock.createMock(JDBCConfigProperties.class);
        Context jndi = EasyMock.createMock(Context.class);

        expect(config.isEnabled()).andReturn(true);
        expectJndi(config, "java:comp/env/jdbc/test");
        expect(jndi.lookup("java:comp/env/jdbc/test")).andStubReturn(ds);
        config.setDatasourceId("java:comp/env/jdbc/test");
        expectLastCall();

        expectVerifyConnect(ds);
        replay(ds, config, jndi);

        DataSourceFactoryBean fact = new DataSourceFactoryBean(config, jndi);

        // Check that we get the DataSource
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds);
        reset(ds);
        replay(ds);

        // Check that the same DataSource is returned on subsequent calls without any changes
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds, config, jndi);

        // Destruction shouldn't do anything to the DataSource
        reset(ds);

        replay(ds);
        fact.destroy();
        verify(ds);
    }

    /** If JNDI lookup fails, fall back to properties file */
    @Test
    public void testJNDIFail() throws Exception {
        final BasicDataSource ds = EasyMock.createMock(BasicDataSource.class);
        JDBCConfigProperties config = EasyMock.createMock(JDBCConfigProperties.class);
        Context jndi = EasyMock.createMock(Context.class);

        expect(config.isEnabled()).andReturn(true);
        expectJndi(config, "java:comp/env/jdbc/test");
        expect(jndi.lookup("java:comp/env/jdbc/test")).andStubThrow(new NamingException());

        expect(config.getJdbcUrl()).andStubReturn(Optional.of("jdbc:test"));
        ds.setUrl("jdbc:test");
        expectLastCall();
        expect(config.getProperty("driverClassName"))
                .andStubReturn("org.geoserver.jdbcconfig.internal.MockJDBCDriver");
        ds.setDriverClassName("org.geoserver.jdbcconfig.internal.MockJDBCDriver");
        expectLastCall();
        expect(config.getProperty("username")).andStubReturn("testUser");
        ds.setUsername("testUser");
        expect(config.getProperty("password")).andStubReturn("swordfish");
        ds.setPassword("swordfish");

        expect(config.getProperty("pool.minIdle")).andStubReturn(null);
        expect(config.getProperty("pool.maxActive")).andStubReturn(null);
        expect(config.getProperty("pool.poolPreparedStatements")).andStubReturn(null);
        expect(config.getProperty("pool.maxOpenPreparedStatements")).andStubReturn(null);
        expect(config.getProperty("pool.testOnBorrow")).andStubReturn(null);

        config.setDatasourceId("jdbc:test");
        expectLastCall();

        ds.setMinIdle(1);
        expectLastCall();
        ds.setMaxActive(10);
        expectLastCall();
        ds.setPoolPreparedStatements(true);
        expectLastCall();
        ds.setMaxOpenPreparedStatements(50);
        expectLastCall();

        expectVerifyConnect(ds);

        replay(ds, config, jndi);

        DataSourceFactoryBean fact =
                new DataSourceFactoryBean(config, jndi) {

                    @Override
                    protected BasicDataSource createBasicDataSource() {
                        return ds;
                    }
                };

        // Check that we get the DataSource
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds);
        reset(ds);
        replay(ds);

        // Check that the same DataSource is returned on subsequent calls without any changes
        assertThat(fact.getObject(), is((DataSource) ds));
        verify(ds, config, jndi);

        // Check that destruction properly closes the DataSource
        reset(ds);
        ds.close();
        expectLastCall();
        replay(ds);
        fact.destroy();
        verify(ds);
    }

    private void expectJndi(JDBCConfigProperties config, String name) {
        expect(config.getProperty("jndiName")).andStubReturn(name);
        expect(config.getJndiName()).andStubReturn(Optional.fromNullable(name));
    }

    private void expectVerifyConnect(DataSource ds) throws Exception {
        Connection conn = createMock(Connection.class);
        // the 2 times expectations are due to checking the database metadata
        // during Dialect initialization
        conn.close();
        expectLastCall();
        DatabaseMetaData metadata = EasyMock.createMock(DatabaseMetaData.class);
        expect(metadata.getDriverName()).andReturn("test");
        expect(conn.getMetaData()).andReturn(metadata);
        replay(conn);
        replay(metadata);
        expect(ds.getConnection()).andReturn(conn);
    }
}
