/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.mosaic;

import com.google.common.base.Preconditions;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geotools.util.logging.Logging;

/**
 * Extracts timestamps from granule file names.
 *
 * <p>This class needs a regular expression in order to extract the raw timestamp from the filename
 * and a date format to parse the exracted timestamp into a date object.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class FilenameTimeHandler extends TimeHandler {

    private static final long serialVersionUID = 1L;

    public static final String FILENAME_REGEX = "filenameRegex";

    public static final String TIME_FORMAT = "timeFormat";

    static Logger LOGGER = Logging.getLogger(FilenameTimeHandler.class);

    String filenameRegex;
    Pattern filenamePattern;

    SimpleDateFormat timeFormat;

    @Override
    public void init(Map<String, Object> props) {
        if (props.containsKey(FILENAME_REGEX)) {
            setFilenameRegex(props.get(FILENAME_REGEX).toString());
        }

        if (props.containsKey(TIME_FORMAT)) {
            setTimeFormat(props.get(TIME_FORMAT).toString());
        }
    }

    public String getFilenameRegex() {
        return filenameRegex;
    }

    public void setFilenameRegex(String filenameRegex) {
        this.filenameRegex = filenameRegex;
        filenamePattern = Pattern.compile(".*(" + filenameRegex + ").*");
    }

    public String getTimeFormat() {
        return timeFormat != null ? timeFormat.toPattern() : null;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = new SimpleDateFormat(timeFormat);
    }

    @Override
    public Date computeTimestamp(Granule g) {
        Preconditions.checkNotNull(filenamePattern);
        Preconditions.checkNotNull(timeFormat);

        String filename = g.getFile().getName();

        // TODO: add a reason for cases why timestamp can't be determined
        Matcher m = filenamePattern.matcher(g.getFile().getName());
        if (!m.matches() || m.groupCount() != 2) {
            // report back message
            String msg =
                    "Failure parsing time from file "
                            + filename
                            + " with pattern "
                            + getFilenameRegex();
            g.setMessage(msg);

            LOGGER.log(Level.WARNING, msg);
            return null;
        }

        try {
            return timeFormat.parse(m.group(1));
        } catch (ParseException e) {
            String msg =
                    "Failure parsing timestamp with pattern "
                            + timeFormat.toPattern()
                            + ": "
                            + e.getLocalizedMessage();
            g.setMessage(msg);

            LOGGER.log(Level.WARNING, msg, e);
            return null;
        }
    }
}
