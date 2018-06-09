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
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;

/**
 * Postgis data setup for the data reference set online test
 *
 * @author Victor Tey, CSIRO Earth Science and Resource Engineering
 */
public class WfsOnlineTestPostgisSetup extends AbstractReferenceDataSetup {
    private final String versiontbl = "data_version";

    private InputStream script;

    private final double scriptVersion = 1.0;

    public WfsOnlineTestPostgisSetup() throws Exception {
        this.script = this.getClass().getResourceAsStream("/RefDataSet/Postgis_Data_ref_set.sql");
    }

    /** Returns PostgisNGDataStoreFactory */
    @Override
    public JDBCDataStoreFactory createDataStoreFactory() {
        return new PostgisNGDataStoreFactory();
    }

    private void runSqlInsertScript() throws Exception {
        DatabaseUtil du = new DatabaseUtil();
        List<String> sqls = du.splitPostgisSQLScript(script);
        sqls.add("set search_path = public;");
        run(du.rebuildAsSingle(sqls));
        setDataVersion(scriptVersion);
    }

    // these private helper class might be useful in the future. feel free to change its access
    // modifier
    private void setDataVersion(double version) throws Exception {
        this.run("DROP TABLE IF EXISTS public." + versiontbl);
        this.run(
                "CREATE TABLE public."
                        + versiontbl
                        + " ("
                        + "name character varying(100) NOT NULL, "
                        + "version double precision,"
                        + "insert_date timestamp without time zone);");
        this.run(
                "insert into public."
                        + versiontbl
                        + "(name,version,insert_date) values('Data reference set',"
                        + version
                        + ",current_timestamp)");
    }

    @Override
    public String getDatabaseID() {
        return "postgis";
    }

    @Override
    public void setUp() throws Exception {
        runSqlInsertScript();
    }

    @Override
    protected Properties createExampleFixture() {
        Properties fixture = new Properties();
        fixture.put("password", "MyPassword");
        fixture.put("passwd", "MyPassword");
        fixture.put("user", "user");
        fixture.put("port", "5432");
        fixture.put("url", "jdbc:postgresql://MyHost/MyDatabase");
        fixture.put("host", "MyHost");
        fixture.put("database", "MyDatabase");
        fixture.put("driver", "org.postgresql.Driver");
        fixture.put("dbtype", "postgisng");
        return fixture;
    }
}
