/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.exec.CommandLine;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ValidationException;

/**
 * Runs gdaladdo on a input raster file
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GdalAddoTransform extends AbstractCommandLinePreTransform implements RasterTransform {
    private static final long serialVersionUID = -6241844409161277128L;

    /** Checks if gdaladdo is available */
    public static boolean isAvailable() throws IOException {
        return new GdalAddoTransform(new ArrayList<String>(), Arrays.asList(2)).checkAvailable();
    }

    private List<Integer> levels;

    public GdalAddoTransform(List<String> options, List<Integer> levels) {
        super(options);
        this.levels = levels;
        if (levels == null || levels.size() == 0) {
            throw new ValidationException("Levels is missing, must contain at least one value");
        } else {
            for (Integer level : levels) {
                if (level == null) {
                    throw new ValidationException(
                            "Invalid null level found in the gdaladdo overviews levels: " + levels);
                }
                if (level <= 1) {
                    throw new ValidationException(
                            "Invalid level found in the gdaladdo overviews levels, they must be positive and greater than one: "
                                    + level);
                }
            }
            int previous = levels.get(0);
            for (int i = 1; i < levels.size(); i++) {
                int curr = levels.get(i);
                if (curr <= previous) {
                    throw new ValidationException(
                            "Invalid levels order, they must be provided in increasing order, but we have "
                                    + curr
                                    + " after "
                                    + previous
                                    + " in "
                                    + levels);
                }
            }
        }
    }

    @Override
    protected void setupCommandLine(boolean inline, CommandLine cmd) {
        super.setupCommandLine(inline, cmd);
        for (Integer level : levels) {
            cmd.addArgument(String.valueOf(level));
        }
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
            throw new IOException("Can run gdaladdo only against file data");
        }
    }

    @Override
    protected File getExecutable() throws IOException {
        return getExecutableFromPath("gdaladdo");
    }

    @Override
    protected boolean isInline() {
        return true;
    }

    @Override
    protected List<String> getAvailabilityTestOptions() {
        return Collections.singletonList("--version");
    }

    /** @return the levels */
    public List<Integer> getLevels() {
        return levels;
    }
}
