/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.jdbc;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.SQLException;
import org.geotools.data.jdbc.datasource.DataSourceFinder;
import org.geotools.data.jdbc.datasource.UnWrapper;
import org.junit.Before;
import org.junit.Test;

/**
 * Uses the known target org.apache.commons.dbcp.DelegatingStatement to test out GenericUnWrapper in
 * isolation.
 *
 * @author Jody Garnett (Boundless)
 */
public class GenericUnWrapperTest {
    GenericUnWrapper unwrapper = new GenericUnWrapper();

    @Before
    public void reset() {
        // reset generic wrapper state
        GenericUnWrapper.CONNECTION_METHODS.remove(WrapperConnection.class);
    }

    @Test
    public void testUnwrapConnection()
            throws SQLException, NoSuchMethodException, SecurityException {
        Connection connection = new TestConnection();
        Connection wrapper = new WrapperConnection(connection);
        assertTrue(wrapper.isWrapperFor(Connection.class));
        Connection unwrap = wrapper.unwrap(Connection.class);

        assertSame(connection, unwrap);

        UnWrapper unwrapper = new GenericUnWrapper();

        assertFalse(unwrapper.canUnwrap(wrapper));
        try {
            assertNull(unwrapper.unwrap(wrapper));
            fail("Cannot unwrap yet");
        } catch (Exception expected) {
        }
        GenericUnWrapper.CONNECTION_METHODS.put(
                WrapperConnection.class,
                WrapperConnection.class.getMethod("getUnderlyingConnection", null));

        assertTrue(unwrapper.canUnwrap(wrapper));
        assertSame(connection, unwrapper.unwrap(wrapper));
    }

    @Test
    public void testSPIRegistration() throws Exception {
        Connection connection = new TestConnection();
        Connection wrapper = new WrapperConnection(connection);

        GenericUnWrapper.CONNECTION_METHODS.put(
                WrapperConnection.class,
                WrapperConnection.class.getMethod("getUnderlyingConnection", null));

        UnWrapper uw = DataSourceFinder.getUnWrapper(wrapper);
        assertNotNull("registed and canUnwrap", uw);
        if (uw instanceof GenericUnWrapper) {
            assertSame("Generic unwrapper is working", connection, uw.unwrap(wrapper));
        }
    }
}
