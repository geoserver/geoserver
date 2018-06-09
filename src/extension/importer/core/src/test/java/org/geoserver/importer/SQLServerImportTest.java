/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.sql.Statement;
import org.junit.Ignore;

@Ignore
public class SQLServerImportTest extends ImporterDbTestBase {

    @Override
    protected String getFixtureId() {
        return "sqlserver";
    }

    @Override
    protected void createWidgetsTable(Statement st) throws Exception {
        String sql =
                "CREATE TABLE widgets (id int IDENTITY(0,1) PRIMARY KEY, "
                        + "geometry geometry, doubleProperty float, stringProperty varchar(255))";
        run(sql, st);

        sql =
                "INSERT INTO widgets (geometry,doubleProperty,stringProperty) VALUES ("
                        + "geometry::STGeomFromText('POINT(0 0)',4326), 1.99,'anvil');";
        run(sql, st);

        sql =
                "INSERT INTO widgets (geometry,doubleProperty,stringProperty) VALUES ("
                        + "geometry::STGeomFromText('POINT(1 1)',4326), 1.99,'bomb');";
        run(sql, st);

        sql =
                "INSERT INTO widgets (geometry,doubleProperty,stringProperty) VALUES ("
                        + "geometry::STGeomFromText('POINT(2 2)',4326), 2.99,'dynamite');";
        run(sql, st);

        // create the spatial index
        run(
                "CREATE SPATIAL INDEX _widgets_geometry_index on widgets(geometry) WITH (BOUNDING_BOX = (-10, -10, 10, 10))",
                st);
    }
}
