package org.geotools.dggs.clickhouse;

import org.geotools.jdbc.JDBCGeometrylessTestSetup;
import org.geotools.jdbc.JDBCTestSetup;

public class ClickHouseGeometrylessTestSetup extends JDBCGeometrylessTestSetup {

    protected ClickHouseGeometrylessTestSetup(JDBCTestSetup delegate) {
        super(delegate);
    }

    @Override
    protected void createPersonTable() throws Exception {
        run(
                "CREATE TABLE GT_PK_METADATA ("
                        + "  table_schema Nullable(String),"
                        + "  table_name String,"
                        + "  pk_column String,"
                        + "  pk_column_idx Int32,"
                        + "  pk_policy String,"
                        + "  pk_sequence Nullable(String)) ENGINE = MergeTree() order by table_name");
        run(
                "INSERT INTO \"GT_PK_METADATA\" VALUES (NULL, 'person', 'fid', '0' , 'assigned', NULL)");

        run(
                "CREATE TABLE \"person\"(\"fid\" Int32, \"id\" Nullable(Int32), "
                        + "\"name\" Nullable(String), \"age\" Nullable(Int32)) "
                        + "ENGINE = MergeTree() "
                        + "ORDER BY fid "
                        + "PARTITION by fid "
                        + "PRIMARY KEY fid");
        run("INSERT INTO \"person\" (\"id\",\"name\",\"age\") VALUES (0,'Paul',32)");
        run("INSERT INTO \"person\" (\"id\",\"name\",\"age\") VALUES (0,'Anne',40)");
    }

    @Override
    protected void dropPersonTable() throws Exception {
        runSafe("DROP TABLE \"person\"");
        runSafe("DROP TABLE \"GT_PK_METADATA\"");
    }

    @Override
    protected void dropZipCodeTable() throws Exception {
        run("DROP TABLE \"zipcode\"");
    }
}
