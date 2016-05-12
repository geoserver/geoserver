/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.jdbc;

public class PostgisJDBCStatusStoreTest extends AbstractJDBCStatusStoreTest {

    @Override
    String getFixtureId() {
        return "wps-postgis";
    }
}
