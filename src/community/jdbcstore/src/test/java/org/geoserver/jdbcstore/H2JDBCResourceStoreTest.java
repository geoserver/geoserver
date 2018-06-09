/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcstore;

import org.junit.Before;

/**
 * @author Kevin Smith, Boundless
 * @author Niels Charlier
 */
public class H2JDBCResourceStoreTest extends AbstractJDBCResourceStoreTest {

    @Before
    public void setUp() throws Exception {
        support = new H2TestSupport();
    }
}
