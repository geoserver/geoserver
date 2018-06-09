/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore;

import org.junit.Before;
import org.junit.Ignore;

/**
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
@Ignore
public class PostgresJDBCResourceStoreTest extends AbstractJDBCResourceStoreTest {

    @Before
    public void setUp() throws Exception {
        support = new PostgresTestSupport();
    }
}
