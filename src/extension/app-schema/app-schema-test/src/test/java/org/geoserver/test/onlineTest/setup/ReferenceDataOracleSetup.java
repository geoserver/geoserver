/* (c) 2014-2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest.setup;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.geoserver.test.onlineTest.support.AbstractReferenceDataSetup;
import org.geoserver.test.onlineTest.support.DatabaseUtil;
import org.geotools.data.oracle.OracleNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

/**
 * Postgis data setup for the data reference set online test
 *
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 */
public class ReferenceDataOracleSetup extends AbstractReferenceDataSetup {
    private final String versiontbl = "data_version".toUpperCase();

    private InputStream script;

    private final double scriptVersion = 1.0;

    @Override
    protected String typeName(String raw) {
        return raw.toUpperCase();
    }

    @Override
    protected String attributeName(String raw) {
        return raw.toUpperCase();
    }

    public ReferenceDataOracleSetup() throws Exception {
        this.script = this.getClass().getResourceAsStream("/RefDataSet/Oracle_Data_ref_set.sql");
    }

    @Override
    public JDBCDataStoreFactory createDataStoreFactory() {
        return new OracleNGDataStoreFactory();
    }

    @Override
    protected Properties createExampleFixture() {
        Properties fixture = new Properties();
        fixture.put("password", "MyPassword");
        fixture.put("passwd", "MyPassword");
        fixture.put("user", "user");
        fixture.put("port", "1521");
        fixture.put("url", "jdbc:oracle:thin:@MyHost:1521:MyDatabase");
        fixture.put("host", "MyHost");
        fixture.put("database", "MyDatabase");
        fixture.put("driver", "oracle.jdbc.driver.OracleDriver");
        fixture.put("dbtype", "Oracle");
        return fixture;
    }

    protected void runSqlInsertScript() throws Exception {
        DatabaseUtil du = new DatabaseUtil();
        List<String> sqls = du.splitOracleSQLScript(script);
        for (String sql : sqls) {
            if (sql.startsWith("CALL")) {
                String formattedSP = "{" + sql + "}";
                this.run(formattedSP);
                continue;
            }
            this.run(sql);
        }
        this.setDataVersion(this.scriptVersion);
    }

    // these private helper class might be useful in the future. feel free to change its access
    // modifier
    private void setDataVersion(double version) throws Exception {
        this.run("{CALL DROP_TABLE('" + versiontbl + "')}");
        this.run(
                "CREATE TABLE "
                        + versiontbl
                        + " ("
                        + "NAME VARCHAR2(100 BYTE) NOT NULL, "
                        + "VERSION NUMBER(25,2),"
                        + "INSERT_DATE DATE)");
        this.run(
                "insert into "
                        + versiontbl
                        + "(name,version,insert_date) values('Data reference set',"
                        + version
                        + ",current_timestamp)");
    }

    @Override
    public String getDatabaseID() {
        return "oracle";
    }

    @Override
    public void setUp() throws Exception {
        runSqlInsertScript();
    }
}
