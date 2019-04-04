/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gdal;

import it.geosolutions.imageio.gdalframework.GDALUtilities;
import java.util.Optional;
import org.gdal.gdal.gdal;
import org.geoserver.platform.ModuleStatus;
import org.geotools.util.Version;
import org.geotools.util.factory.GeoTools;

/** Status page checking availability and binary details */
public class GDALStatus implements ModuleStatus {

    @Override
    public String getModule() {
        return "gs-gdal";
    }

    @Override
    public Optional<String> getComponent() {
        return Optional.ofNullable("GridCoverage2DReader");
    }

    @Override
    public String getName() {
        return "ImageI/O-Ext GDAL Coverage Extension";
    }

    @Override
    public Optional<String> getVersion() {
        Version v = GeoTools.getVersion(GDALUtilities.class);
        if (v == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(v.toString());
    }

    @Override
    public boolean isAvailable() {
        return GDALUtilities.isGDALAvailable();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Optional<String> getMessage() {
        String message = "JNI GDAL Wrapper Version: " + getGDALWrapperJarVersion().orElse("null");
        if (!isAvailable()) {
            message += "\njava.library.path: " + System.getProperty("java.library.path", "");
        } else {
            message += metaData();
        }
        return Optional.ofNullable(message);
    }

    @Override
    public Optional<String> getDocumentation() {
        return Optional.ofNullable("");
    }

    public Optional<String> getGDALWrapperJarVersion() {
        if (isAvailable()) {
            Version v = GeoTools.getVersion(gdal.class);
            if (v == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(v.toString());
        } else {
            return Optional.ofNullable("unavailable");
        }
    }

    String metaData() {
        StringBuffer msg = new StringBuffer();
        msg.append("\nGDAL Version: " + gdal.VersionInfo("RELEASE_NAME"));
        msg.append("\nGDAL Release Date: " + gdal.VersionInfo("RELEASE_DATE"));
        msg.append("\nGDAL Build Info: " + gdal.VersionInfo("BUILD_INFO"));
        return msg.toString();
    }
}
