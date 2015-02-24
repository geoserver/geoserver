/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DbMappingsTest extends TestCase {

    private JDBCConfigTestSupport testSupport;

    @Override
    protected void setUp() throws Exception {
        testSupport = new JDBCConfigTestSupport();
        testSupport.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        testSupport.tearDown();
    }

    public void testInitDb() throws Exception {
        DataSource dataSource = testSupport.getDataSource();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        DbMappings dbInit = new DbMappings();
        dbInit.initDb(template);
    }
}
