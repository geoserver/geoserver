/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.util.Map;
import java.util.Optional;
import org.geoserver.ogr.core.OGRModuleStatus;
import org.geoserver.platform.ModuleStatus;

public class OGRWFSModuleStatus extends OGRModuleStatus implements ModuleStatus {

    private final Ogr2OgrOutputFormat format;

    private String checkedConfiguration;

    private boolean available;

    public OGRWFSModuleStatus(Ogr2OgrOutputFormat format) {
        this.format = format;
    }

    @Override
    public String getModule() {
        return "gs-ogr-wfs";
    }

    @Override
    public String getName() {
        return "OGR WFS Module";
    }

    @Override
    public synchronized boolean isAvailable() {
        String configuration = format.getExecutable() + format.getEnvironment();
        if (!configuration.equals(checkedConfiguration)) {
            available = new OGRWrapper(format.getExecutable(), format.getEnvironment()).isAvailable();
            checkedConfiguration = configuration;
        }
        return available;
    }

    @Override
    public Optional<String> getMessage() {
        Map<String, String> environment = format.getEnvironment();
        String gdalData = environment != null && environment.get("GDAL_DATA") != null
                ? environment.get("GDAL_DATA")
                : System.getenv("GDAL_DATA");
        return Optional.of("ogr2ogr executable: " + format.getExecutable() + "\nGDAL_DATA: " + gdalData);
    }
}
