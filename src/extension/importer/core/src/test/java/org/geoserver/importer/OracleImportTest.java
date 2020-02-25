/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.sql.Statement;
import org.junit.Ignore;

@Ignore
public class OracleImportTest extends ImporterDbTestBase {

    @Override
    protected String getFixtureId() {
        return "oracle";
    }

    @Override
    protected String tableName(String name) {
        return name.toUpperCase();
    }

    @Override
    protected void dropTable(String tableName, Statement st) throws Exception {
        runSafe("DROP TABLE " + tableName(tableName) + " PURGE", st);
        runSafe("DROP SEQUENCE " + tableName(tableName) + "_PKEY_SEQ", st);
        run(
                "DELETE FROM USER_SDO_GEOM_METADATA WHERE TABLE_NAME = '"
                        + tableName(tableName)
                        + "'",
                st);
    }

    protected void createWidgetsTable(Statement st) throws Exception {
        String sql =
                "CREATE TABLE WIDGETS ("
                        + "ID INT, GEOMETRY MDSYS.SDO_GEOMETRY, "
                        + "PRICE FLOAT, DESCRIPTION VARCHAR(255), "
                        + "PRIMARY KEY(id))";
        run(sql, st);

        sql = "CREATE SEQUENCE WIDGETS_PKEY_SEQ";
        run(sql, st);

        sql =
                "INSERT INTO USER_SDO_GEOM_METADATA (TABLE_NAME, COLUMN_NAME, DIMINFO, SRID ) "
                        + "VALUES ('WIDGETS','GEOMETRY', MDSYS.SDO_DIM_ARRAY(MDSYS.SDO_DIM_ELEMENT('X',-180,180,0.5), "
                        + "MDSYS.SDO_DIM_ELEMENT('Y',-90,90,0.5)), 4326)";
        run(sql, st);

        sql =
                "CREATE INDEX WIDGETS_GEOMETRY_IDX ON WIDGETS(GEOMETRY) "
                        + "INDEXTYPE IS MDSYS.SPATIAL_INDEX PARAMETERS ('SDO_INDX_DIMS=2 LAYER_GTYPE=\"POINT\"')";
        run(sql, st);

        sql =
                "INSERT INTO WIDGETS VALUES (0,"
                        + "MDSYS.SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(0.0,0.0,NULL),NULL,NULL), 1.99, 'anvil')";
        run(sql, st);

        sql =
                "INSERT INTO WIDGETS VALUES (1,"
                        + "MDSYS.SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(1.0,1.0,NULL),NULL,NULL), 2.99, 'bomb')";
        run(sql, st);

        sql =
                "INSERT INTO WIDGETS VALUES (2,"
                        + "MDSYS.SDO_GEOMETRY(2001,4326,SDO_POINT_TYPE(2.0,2.0,NULL),NULL,NULL), 3.99, 'dynamite')";
        run(sql, st);
    };
}
