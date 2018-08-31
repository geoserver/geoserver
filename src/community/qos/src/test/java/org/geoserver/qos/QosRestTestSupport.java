/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos;

import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AccessMode;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import org.junit.After;
import org.junit.Before;

public abstract class QosRestTestSupport extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");
    }

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @After
    public void revertChanges() {
        revertService(WMSInfo.class, null);
    }

    protected String getFileData(String filename) {
        String result = null;
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            result =
                    IOUtils.toString(
                            classLoader.getResourceAsStream(filename), Charset.defaultCharset());
        } catch (IOException e) {
        }
        return result;
    }
}
