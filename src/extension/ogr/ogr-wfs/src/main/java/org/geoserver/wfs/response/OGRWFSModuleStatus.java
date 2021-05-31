/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Properties;
import org.geoserver.ogr.core.OGRModuleStatus;
import org.geoserver.platform.ModuleStatus;

public class OGRWFSModuleStatus extends OGRModuleStatus implements ModuleStatus {
    private static Boolean IS_OGR_AVAILABLE = null;
    private static String OGR2OGR;
    private static String GDAL_DATA;

    @Override
    public String getModule() {
        return "gs-ogr-wfs";
    }

    @Override
    public String getName() {
        return "OGR WFS Module";
    }

    @Override
    public boolean isAvailable() {
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
            }
        }

        return IS_OGR_AVAILABLE;
    }
}
