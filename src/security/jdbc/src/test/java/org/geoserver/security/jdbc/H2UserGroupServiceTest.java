/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.Statement;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.junit.Test;

public class H2UserGroupServiceTest extends JDBCUserGroupServiceTest {

    @Override
    protected String getFixtureId() {
        return "h2";
    }

    @Override
    protected JDBCUserGroupServiceConfig createConfigObject(String serviceName) {
        return JDBCTestSupport.createConfigObjectH2(serviceName, getSecurityManager());
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {
        return JDBCTestSupport.createH2UserGroupService(getFixtureId(), getSecurityManager());
    }

    /** A property column that is not character data is still read back as its text form. */
    @Test
    public void testPropertyValueReadAsText() throws Exception {
        store.addUser(store.createUserObject("ranked", "pw", true));
        store.store();

        try (Connection con = ((AbstractJDBCService) service).getDataSource().getConnection();
                Statement st = con.createStatement()) {
            con.setAutoCommit(true);
            st.execute("drop table user_props");
            st.execute("create table user_props(username varchar(128) not null, "
                    + "propname varchar(64) not null, propvalue integer, primary key (username, propname))");
            st.execute("insert into user_props values ('ranked', 'rank', 7)");
        }

        GeoServerUser user = service.getUserByUsername("ranked");
        Object value = user.getProperties().get("rank");
        assertThat(value, instanceOf(String.class));
        assertEquals("7", value);
    }
}
