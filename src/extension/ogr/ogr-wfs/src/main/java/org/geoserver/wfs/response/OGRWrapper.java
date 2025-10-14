/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.ogr.core.AbstractToolWrapper;
import org.geoserver.ogr.core.Format;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.util.logging.Logging;

/**
 * Helper used to invoke ogr2ogr
 *
 * @author Andrea Aime - OpenGeo
 * @author Stefano Costa - GeoSolutions
 */
public class OGRWrapper extends AbstractToolWrapper {

    private static final Logger LOGGER = Logging.getLogger(OGRWrapper.class);

    private File crsFile;

    public OGRWrapper(String ogrExecutable, Map<String, String> environment) {
        super(ogrExecutable, environment);
    }

    @Override
    public String getToolFormatParameter() {
        return "-f";
    }

    @Override
    public boolean isInputFirst() {
        return false;
    }

    /** Returns a list of the ogr2ogr supported formats */
    @Override
    public Set<String> getSupportedFormats() {
        try {
            // this one works up to ogr2ogr 1.7
            List<String> commands = new ArrayList<>();
            commands.add(getExecutable());
            commands.add("--help");

            Set<String> formats = new HashSet<>();
            addFormats(commands, formats);

            // this one is required starting with ogr2ogr 1.8
            commands = new ArrayList<>();
            commands.add(getExecutable());
            commands.add("--long-usage");
            addFormats(commands, formats);

            // this one is required starting with ogr2ogr 3.
            commands = new ArrayList<>();
            commands.add(getExecutable());
            commands.add("--formats");
            addFormats(commands, formats);

            return formats;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not get the list of output formats supported by ogr2ogr", e);
            return Collections.emptySet();
        }
    }

    private void addFormats(List<String> commands, Set<String> formats) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        // can't trust the exit code, --help exits with -1 on my pc
        run(commands, sb);

        String[] lines = sb.toString().split("\n");
        if (commands.contains("--help") || commands.contains("--long-usage")) {
            Pattern helpPattern = Pattern.compile("\\s*-f \".*");
            for (String line : lines) {
                if (helpPattern.matcher(line).matches()) {
                    String format = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
                    formats.add(format);
                }
            }
        }
        if (commands.contains("--formats")) {
            Pattern formatPattern = Pattern.compile("^  (.*) -.*$");
            for (String line : lines) {
                Matcher match = formatPattern.matcher(line);
                if (match.matches()) {
                    formats.add(match.group(1));
                }
            }
        }
    }

    /** Returns true if ogr2ogr is available, that is, if executing "ogr2ogr --version" returns 0 as the exit code */
    @Override
    public boolean isAvailable() {
        List<String> commands = new ArrayList<>();
        commands.add(getExecutable());
        commands.add("--version");

        try {
            return run(commands, null) == 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, getExecutable() + " is not available", e);
            return false;
        }
    }

    @Override
    public void onBeforeRun(
            List<String> cmd,
            File inputData,
            File outputDirectory,
            String typeName,
            Format format,
            CoordinateReferenceSystem crs)
            throws IOException {
        crsFile = dumpCrs(inputData.getParentFile(), crs);

        if (crsFile != null) {
            cmd.add("-a_srs");
            cmd.add(crsFile.getAbsolutePath());
        }
    }

    @Override
    public void onAfterRun(int exitCode) throws IOException {
        if (crsFile != null) {
            crsFile.delete();
            crsFile = null;
        }
    }
}
