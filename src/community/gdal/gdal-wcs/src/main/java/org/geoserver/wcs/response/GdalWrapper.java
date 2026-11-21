/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

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
import org.geotools.util.logging.Logging;

/**
 * Helper used to invoke gdal_translate.
 *
 * @author Stefano Costa, GeoSolutions
 */
public class GdalWrapper extends AbstractToolWrapper {

    private static final Logger LOGGER = Logging.getLogger(GdalWrapper.class);

    private static final Pattern FORMAT = Pattern.compile("^\\s+(\\S+)(?: -[^(]*-)? \\(([^)]*)\\):");

    public GdalWrapper(String executable, Map<String, String> environment) {
        super(executable, environment);
    }

    /**
     * Returns the gdal_translate output formats (i.e. what must be passed to gdal_translate via its -of parameter),
     * parsed from <code>--formats</code>.
     */
    @Override
    public Set<String> getSupportedFormats() {
        try {
            StringBuilder sb = new StringBuilder();
            run(List.of(getExecutable(), "--formats"), sb);
            return parseFormats(sb.toString());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not get the list of output formats supported by gdal_translate", e);
            return Collections.emptySet();
        }
    }

    /** Parses <code>--formats</code> output, keeping the drivers that can write. */
    static Set<String> parseFormats(String output) {
        Set<String> formats = new HashSet<>();
        for (String line : output.split("\\R")) {
            Matcher matcher = FORMAT.matcher(line);
            if (matcher.find() && matcher.group(2).contains("w")) {
                formats.add(matcher.group(1));
            }
        }
        return formats;
    }

    /**
     * Returns true if gdal_translate is available, that is, if executing "gdal_translate --version" returns 0 as the
     * exit code.
     */
    @Override
    public boolean isAvailable() {
        List<String> commands = new ArrayList<>();
        commands.add(getExecutable());
        commands.add("--version");

        try {
            return run(commands, null) == 0;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "gdal_translate is not available", e);
            return false;
        }
    }

    @Override
    public String getToolFormatParameter() {
        return "-of";
    }
}
