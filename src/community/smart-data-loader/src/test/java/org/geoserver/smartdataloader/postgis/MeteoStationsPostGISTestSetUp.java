package org.geoserver.smartdataloader.postgis;

import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;

public class MeteoStationsPostGISTestSetUp extends PostGISTestSetUp {

    protected String METEOS_SQL_SCRIPT = "meteo_db.sql";

    public MeteoStationsPostGISTestSetUp() {}

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        setUpData();
    }

    @Override
    protected void setUpData() throws Exception {
        super.setUpData();

        String sql =
                IOUtils.toString(
                        getClass().getResourceAsStream("./mockdata/" + METEOS_SQL_SCRIPT),
                        Charset.defaultCharset());
        run(sql);
    }

    @After
    @Override
    public void tearDown() throws Exception {
        dropSchema();
        if (!getDataSource().getConnection().isClosed()) getDataSource().getConnection().close();
    }
}
