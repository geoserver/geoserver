/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.regex.Pattern;

public final class FrequencyUtil {

    public static final Pattern DAILY_PATTERN =
            Pattern.compile("^00? (\\d\\d?) (\\d\\d?) \\* \\* \\?$");
    public static final Pattern WEEKLY_PATTERN =
            Pattern.compile("^00? (\\d\\d?) (\\d\\d?) \\? \\* (\\p{Alpha}\\p{Alpha}\\p{Alpha})$");
    public static final Pattern MONTHLY_PATTERN =
            Pattern.compile("^00? (\\d\\d?) (\\d\\d?) (\\d\\d?) \\* \\?$");

    private FrequencyUtil() {}

    public static DayOfWeek findDayOfWeek(String threeLetters) {
        for (DayOfWeek dow : DayOfWeek.values()) {
            if (threeLetters.equals(dow.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))) {
                return dow;
            }
        }
        return null;
    }
}
