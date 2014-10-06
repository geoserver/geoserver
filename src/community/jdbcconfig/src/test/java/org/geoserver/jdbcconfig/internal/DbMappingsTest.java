/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@RunWith(Parameterized.class)
public class DbMappingsTest {

    private JDBCConfigTestSupport testSupport;

    public DbMappingsTest(JDBCConfigTestSupport.DBConfig dbConfig) {
        testSupport = new JDBCConfigTestSupport(dbConfig);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return JDBCConfigTestSupport.parameterizedDBConfigs();
    }

    @Before
    public void setUp() throws Exception {
        testSupport.setUp();
    }

    @After
    public void tearDown() throws Exception {
        testSupport.tearDown();
    }

    @Test
    public void testInitDb() throws Exception {
        DataSource dataSource = testSupport.getDataSource();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        DbMappings dbInit = new DbMappings(new Dialect());
        dbInit.initDb(template);
    }
}
