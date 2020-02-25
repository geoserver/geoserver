/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.csw.CSWTestSupport;
import org.geoserver.data.test.SystemTestData;

public class CSWInternalTestSupport extends CSWTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {

        System.setProperty(
                "DefaultCatalogStore",
                "org.geoserver.csw.store.internal.GeoServerInternalCatalogStore");

        // copy all mappings into the data directory
        File root = testData.getDataDirectoryRoot();
        File csw = new File(root, "csw");
        File records = new File("./src/test/resources/org/geoserver/csw/store/internal");
        FileUtils.copyDirectory(records, csw);

        super.setUpTestData(testData);
    }
}
