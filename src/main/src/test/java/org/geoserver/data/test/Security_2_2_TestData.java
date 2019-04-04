/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.test;

import java.io.File;
import java.io.IOException;
import org.geoserver.util.IOUtils;

/**
 * Test data for migration from 2.2 -> 2.3
 *
 * @author mcr
 */
public class Security_2_2_TestData extends SystemTestData {

    public Security_2_2_TestData() throws IOException {
        super();
    }

    @Override
    public void setUpSecurity() throws IOException {
        File secDir = new File(getDataDirectoryRoot(), "security");
        IOUtils.decompress(
                Security_2_2_TestData.class.getResourceAsStream("security-2.2.zip"), secDir);
    }
}
