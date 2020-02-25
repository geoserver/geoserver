/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Common interface for helper classes wrapping an external tool.
 *
 * @author Stefano Costa, GeoSolutions
 */
public interface ToolWrapper {

    /** Returns the full path to the executable to run. */
    public String getExecutable();

    /** Returns the environment variables that should be set prior to invoking the tool. */
    public Map<String, String> getEnvironment();

    /** Returns the command line parameter used to specify the output format. */
    public String getToolFormatParameter();

    /**
     * If true, the input file should precede the output file in the list of arguments passed to the
     * tool.
     *
     * @return <code>true</code> if input comes first, <code>false</code> otherwise
     */
    public boolean isInputFirst();

    /** Returns a list of the tool supported formats */
    public Set<String> getSupportedFormats();

    /** Returns true if the specified executable command is available and can be run. */
    public boolean isAvailable();

    /**
     * Performs the conversion, returns the (main) output file
     *
     * @param inputData the input file
     * @param outputDirectory the output directory
     * @param typeName the type name
     * @param format the format descriptor
     * @param crs the coordinate reference system of the output
     * @return the output file
     */
    public File convert(
            File inputData,
            File outputDirectory,
            String typeName,
            Format format,
            CoordinateReferenceSystem crs)
            throws IOException, InterruptedException;
}
