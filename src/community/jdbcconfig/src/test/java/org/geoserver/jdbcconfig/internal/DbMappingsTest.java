/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class DbMappingsTest extends TestCase {

    private JdbcConfigTestSupport testSupport;

    @Override
    protected void setUp() throws Exception {
        testSupport = new JdbcConfigTestSupport();
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
