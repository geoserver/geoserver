/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

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
import org.geotools.util.logging.Logging;

/**
 * Helper used to invoke gdal_translate.
 *
 * @author Stefano Costa, GeoSolutions
 */
public class GdalWrapper extends AbstractToolWrapper {

    private static final Logger LOGGER = Logging.getLogger(GdalWrapper.class);

    public GdalWrapper(String executable, Map<String, String> environment) {
        super(executable, environment);
    }

    /**
     * Returns a list of the gdal_translate supported formats (i.e. what must be passed to
     * gdal_translate via its -of parameter)
     */
    public Set<String> getSupportedFormats() {
        try {
            // this works with gdal_translate v. 1.11.2
            // TODO: test with other GDAL versions
            List<String> commands = new ArrayList<String>();
            commands.add(getExecutable());
            commands.add("--long-usage");

            Set<String> formats = new HashSet<String>();
            addFormats(commands, formats);

            return formats;
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Could not get the list of output formats supported by gdal_translate",
                    e);
            return Collections.emptySet();
        }
    }

    /**
     * Runs the provided command and parses its output to extract a set of supported formats.
     *
     * @param commands the command to run
     * @param formats the parsed formats will be added to this set
     */
    private void addFormats(List<String> commands, Set<String> formats)
            throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        run(commands, sb);

        Pattern formatRegExp = Pattern.compile("^\\s{2}(\\w+)\\:\\s");
        String[] lines = sb.toString().split("\n");
        for (String line : lines) {
            Matcher formatMatcher = formatRegExp.matcher(line);
            if (formatMatcher.find()) {
                String format = formatMatcher.group(1);
                formats.add(format);
            }
        }
    }

    /**
     * Returns true if gdal_translate is available, that is, if executing "gdal_translate --version"
     * returns 0 as the exit code.
     */
    public boolean isAvailable() {
        List<String> commands = new ArrayList<String>();
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
