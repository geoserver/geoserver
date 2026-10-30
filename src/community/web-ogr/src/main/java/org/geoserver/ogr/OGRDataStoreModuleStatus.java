/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr;

import java.util.Optional;
import org.gdal.gdal.gdal;
import org.geoserver.platform.ModuleStatusImpl;
import org.geotools.data.ogr.jni.JniOGRDataStoreFactory;
import org.geotools.util.factory.GeoTools;

/** OGR DataStore status, available when the GDAL Java native library (gdalalljni or gdaljni) can be loaded. */
public class OGRDataStoreModuleStatus extends ModuleStatusImpl {

    public OGRDataStoreModuleStatus() {
        super("gs-web-ogr", "OGR DataStore", "OGR (GDAL) DataStore");
        setCategory(Category.COMMUNITY);
        setDocumentation("community/ogr-store/index.html");
    }

    @Override
    public boolean isAvailable() {
        return new JniOGRDataStoreFactory().isAvailable();
    }

    @Override
    public Optional<String> getMessage() {
        String message = "JNI GDAL Wrapper Version: " + GeoTools.getVersion(gdal.class);
        if (!isAvailable()) {
            return Optional.of(message + "\nGDAL native library not found\njava.library.path: "
                    + System.getProperty("java.library.path"));
        }
        return Optional.of(message + "\nGDAL version: " + gdal.VersionInfo("RELEASE_NAME") + "\nGDAL_DATA: "
                + System.getenv("GDAL_DATA"));
    }
}
