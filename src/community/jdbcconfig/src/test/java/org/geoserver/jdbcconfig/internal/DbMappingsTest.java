/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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

    @Test
    public void testProperties() throws Exception {
        DataSource dataSource = testSupport.getDataSource();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        // Getting the DB mappings
        DbMappings db = new DbMappings(new Dialect());
        db.initDb(template);
        // Getting the properties for the LayerInfo class
        // Initial mock classes
        LayerInfoImpl info = new LayerInfoImpl();
        CoverageInfoImpl resource = new CoverageInfoImpl(null);
        resource.setName("test");
        resource.setTitle("test");
        CoverageStoreInfoImpl store = new CoverageStoreInfoImpl(null);
        store.setName("test");
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName("test");
        store.setWorkspace(workspace);
        resource.setStore(store);
        info.setResource(resource);

        Iterable<Property> properties = db.properties(info);

        boolean titleExists = false;
        boolean prefixedNameExists = false;
        // Iterate on the properties
        for (Property prop : properties) {
            if (prop.getPropertyName().equals("title")) {
                titleExists = true;
            } else if (prop.getPropertyName().equals("prefixedName")) {
                prefixedNameExists = true;
            }
        }
        // Assertions
        assertTrue("title property not found", titleExists);
        assertTrue("prefixedName property not found", prefixedNameExists);
    }
}
