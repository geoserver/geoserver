/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

public class GeoPackageUtil {

    public static boolean isGeopkgTest() {
        String onlineTestId = System.getProperty("testDatabase");
        if (onlineTestId != null && onlineTestId.equals("geopkg")) {
            return true;
        }
        return false;
    }
}
