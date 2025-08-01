/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.visitors.JDBCCyclicAppSchemaVisitorTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostgisCyclicAppSchemaVisitorTest extends JDBCCyclicAppSchemaVisitorTest {

    public PostgisCyclicAppSchemaVisitorTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp("meteo_db_cyclic.sql");
    }
}
