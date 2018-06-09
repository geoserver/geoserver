/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

public class Ogr2OgrTestUtil {
    private static Logger LOGGER = Logging.getLogger(Ogr2OgrTestUtil.class);

    private static Boolean IS_OGR_AVAILABLE;
    private static String OGR2OGR;
    private static String GDAL_DATA;

    public static boolean isOgrAvailable() {

        // check this just once
        if (IS_OGR_AVAILABLE == null) {
            try {
                File props = new File("./src/test/resources/ogr2ogr.properties");
                Properties p = new Properties();
                p.load(new FileInputStream(props));

                OGR2OGR = p.getProperty("ogr2ogr");
                // assume it's in the path if the property file hasn't been configured
                if (OGR2OGR == null) OGR2OGR = "ogr2ogr";
                GDAL_DATA = p.getProperty("gdalData");

                OGRWrapper ogr =
                        new OGRWrapper(OGR2OGR, Collections.singletonMap("GDAL_DATA", GDAL_DATA));
                IS_OGR_AVAILABLE = ogr.isAvailable();
            } catch (Exception e) {
                IS_OGR_AVAILABLE = false;
                e.printStackTrace();
                LOGGER.log(
                        Level.SEVERE,
                        "Disabling ogr2ogr output format tests, as ogr2ogr lookup failed",
                        e);
            }
        }

        return IS_OGR_AVAILABLE;
    }

    public static String getOgr2Ogr() {
        if (isOgrAvailable()) return OGR2OGR;
        else return null;
    }

    public static String getGdalData() {
        if (isOgrAvailable()) return GDAL_DATA;
        else return null;
    }
}
