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
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.impl.GeoServerRole;
import org.junit.Test;

public class H2RoleServiceTest extends JDBCRoleServiceTest {

    @Override
    protected String getFixtureId() {
        return "h2";
    }

    @Override
    public GeoServerRoleService createRoleService(String serviceName) throws Exception {
        return JDBCTestSupport.createH2RoleService(getFixtureId(), getSecurityManager());
    }

    /** A property column that is not character data is still read back as its text form. */
    @Test
    public void testPropertyValueReadAsText() throws Exception {
        store.addRole(store.createRoleObject("ROLE_RANKED"));
        store.store();

        try (Connection con = ((AbstractJDBCService) service).getDataSource().getConnection();
                Statement st = con.createStatement()) {
            con.setAutoCommit(true);
            st.execute("drop table role_props");
            st.execute("create table role_props(rolename varchar(64) not null, "
                    + "propname varchar(64) not null, propvalue integer, primary key (rolename, propname))");
            st.execute("insert into role_props values ('ROLE_RANKED', 'rank', 7)");
        }

        GeoServerRole role = service.getRoleByName("ROLE_RANKED");
        Object value = role.getProperties().get("rank");
        assertThat(value, instanceOf(String.class));
        assertEquals("7", value);
    }
}
