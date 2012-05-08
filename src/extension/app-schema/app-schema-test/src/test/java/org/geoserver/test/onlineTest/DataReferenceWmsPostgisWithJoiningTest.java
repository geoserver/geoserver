package org.geoserver.test.onlineTest;

import junit.framework.Test;

import org.geotools.data.complex.AppSchemaDataAccessRegistry;

public class DataReferenceWmsPostgisWithJoiningTest extends DataReferenceWmsPostgisTest {

    public DataReferenceWmsPostgisWithJoiningTest() throws Exception {
        super();
    }

    /**
     * Read-only test so can use one-time setup.
     *
     * @return
     */
    public static Test suite() {
        try {
            return new OneTimeTestSetup(new DataReferenceWmsPostgisWithJoiningTest());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        AppSchemaDataAccessRegistry.getAppSchemaProperties().setProperty ("app-schema.joining", "true");
        super.oneTimeSetUp();
    }

}
