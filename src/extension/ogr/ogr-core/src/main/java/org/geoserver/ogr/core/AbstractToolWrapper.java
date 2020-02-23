/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogr.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Base class for helpers used to invoke an external tool.
 *
 * @author Andrea Aime, GeoSolutions
 * @author Stefano Costa, GeoSolutions
 */
public abstract class AbstractToolWrapper implements ToolWrapper {

    private String executable;
    private Map<String, String> environment;

    public AbstractToolWrapper(String executable, Map<String, String> environment) {
        this.executable = executable;
        this.environment = new HashMap<String, String>();
        if (environment != null) {
            this.environment.putAll(environment);
        }
    }

    @Override
    public String getExecutable() {
        return executable;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return new HashMap<String, String>(environment);
    }

    @Override
    public boolean isInputFirst() {
        return true;
    }

    @Override
    public File convert(
            File inputData,
            File outputDirectory,
            String typeName,
            Format format,
            CoordinateReferenceSystem crs)
            throws IOException, InterruptedException {
        // build the command line
        List<String> cmd = new ArrayList<String>();
        cmd.add(executable);

        String toolFormatParameter = getToolFormatParameter();
        if (toolFormatParameter != null) {
            cmd.add(toolFormatParameter);
            cmd.add(format.getToolFormat());
        }

        if (format.getOptions() != null) {
            for (String option : format.getOptions()) {
                cmd.add(option);
            }
        }

        StringBuilder sb = new StringBuilder();
        String outFileName = null;
        int exitCode = -1;
        try {
            onBeforeRun(cmd, inputData, outputDirectory, typeName, format, crs);

            outFileName = setInputOutput(cmd, inputData, outputDirectory, typeName, format);

            exitCode = run(cmd, sb);
        } finally {
            onAfterRun(exitCode);
        }

        if (exitCode != 0)
            throw new IOException(
                    executable
                            + " did not terminate successfully, exit code "
                            + exitCode
                            + ". Was trying to run: "
                            + cmd
                            + "\nResulted in:\n"
                            + sb);

        // output may be a directory, handle that case gracefully
        File output = new File(outputDirectory, outFileName);
        if (output.isDirectory()) {
            output = new File(output, outFileName);
        }
        return output;
    }

    /**
     * Sets up input and output parameters.
     *
     * <p>Uses {@link #isInputFirst()} internally to determine whether input or output should come
     * first in the list of arguments.
     *
     * <p>May be overridden by subclasses, e.g. to support commands that modify the input file
     * inline and thus need no output parameter.
     *
     * @param cmd the command to run and its arguments
     * @param inputData the input file
     * @param outputDirectory the output directory
     * @param typeName the type name
     * @param format the format descriptor
     * @return the name of the (main) output file
     */
    protected String setInputOutput(
            List<String> cmd,
            File inputData,
            File outputDirectory,
            String typeName,
            Format format) {
        String outFileName = typeName;

        if (format.getFileExtension() != null) outFileName += format.getFileExtension();
        if (isInputFirst()) {
            cmd.add(inputData.getAbsolutePath());
            cmd.add(new File(outputDirectory, outFileName).getAbsolutePath());
        } else {
            cmd.add(new File(outputDirectory, outFileName).getAbsolutePath());
            cmd.add(inputData.getAbsolutePath());
        }

        return outFileName;
    }

    /**
     * Utility method to dump a {@link CoordinateReferenceSystem} to a temporary file on disk.
     *
     * @return the temp file containing the CRS definition in WKT format
     */
    protected static File dumpCrs(File parentDir, CoordinateReferenceSystem crs)
            throws IOException {
        File crsFile = null;
        if (crs != null) {
            // we don't use an EPSG code since there is no guarantee we'll be able to reverse
            // engineer one. Using WKT also ensures the EPSG params such as the TOWGS84 ones are
            // not lost in the conversion
            // We also write to a file because some operating systems cannot take arguments with
            // quotes and spaces inside (and/or ProcessBuilder is not good enough to escape them)
            crsFile = File.createTempFile("srs", "wkt", parentDir);
            String s = crs.toWKT();
            s = s.replaceAll("\n\r", "").replaceAll("  ", "");
            FileUtils.writeStringToFile(crsFile, s, "UTF-8");
        }

        return crsFile;
    }

    /**
     * Invoked by <code>convert()</code> before the command is actually run, but after the options
     * specified in the format configuration have been added to the arguments list.
     *
     * <p>Default implementation does nothing at all. May be implemented by subclasses to append
     * additional arguments to <code>cmd</code>.
     *
     * @param cmd the command to run and its arguments
     */
    protected void onBeforeRun(
            List<String> cmd,
            File inputData,
            File outputDirectory,
            String typeName,
            Format format,
            CoordinateReferenceSystem crs)
            throws IOException {
        // default implementation does nothing
    }

    /**
     * Invoked by <code>convert()</code> after the command is run. Invocation is done inside a
     * <code>try ... finally</code> block, so it happens even if an exception is thrown during
     * command execution.
     *
     * <p>Default implementation does nothing at all. May be implemented by subclasses to do some
     * clean-up work.
     *
     * @param exitCode the exit code of the invoked command. Usually, 0 indicates normal termination
     */
    protected void onAfterRun(int exitCode) throws IOException {
        // default implementation does nothing
    }

    /**
     * Runs the specified command appending the output to the string builder and returning the exit
     * code.
     *
     * @param cmd the command to run and its arguments
     * @param sb command output is appended here
     * @return the exit code of the invoked command. Usually, 0 indicates normal termination
     */
    protected int run(List<String> cmd, StringBuilder sb) throws IOException, InterruptedException {
        // run the process and grab the output for error reporting purposes
        ProcessBuilder builder = new ProcessBuilder(cmd);
        if (environment != null) builder.environment().putAll(environment);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (sb != null) {
                sb.append("\n");
                sb.append(line);
            }
        }
        return p.waitFor();
    }
}
