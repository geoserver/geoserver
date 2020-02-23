/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportData;

/**
 * Runs gdal_translate on a input raster file
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GdalTranslateTransform extends AbstractCommandLinePreTransform
        implements RasterTransform {
    private static final long serialVersionUID = -6241844409161277128L;

    /** Checks if gdal_translate is available */
    public static boolean isAvailable() throws IOException {
        return new GdalTranslateTransform(new ArrayList<String>()).checkAvailable();
    }

    public GdalTranslateTransform(List<String> options) {
        super(options);
    }

    @Override
    protected List<String> getReplacementTargetNames(ImportData data) throws IOException {
        File input = getInputFile(data);
        return Collections.singletonList(input.getName());
    }

    @Override
    protected File getInputFile(ImportData data) throws IOException {
        if (data instanceof FileData) {
            FileData fd = (FileData) data;
            return fd.getFile();
        } else {
            throw new IOException("Can run gdal_translate only against file data");
        }
    }

    @Override
    protected File getExecutable() throws IOException {
        return getExecutableFromPath("gdal_translate");
    }

    protected List<String> getAvailabilityTestOptions() {
        return Collections.singletonList("--version");
    }
}
