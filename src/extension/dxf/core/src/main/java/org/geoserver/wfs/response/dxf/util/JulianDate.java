/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response.dxf.util;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Julian Date Converter. Formulas got from http://en.wikipedia.org/wiki/Julian_day
 *
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 */
public class JulianDate {
    /** Converts a Date to JD format. */
    public static double toJulian(Date dt) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(dt);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // precalc some parameters
        double a = Math.floor((14 - month) / 12);
        double y = year + 4800 - a;
        double m = month + 12 * a - 3;

        // julian day number
        double jdn =
                day
                        + Math.floor((153 * m + 2) / 5)
                        + 365 * y
                        + Math.floor(y / 4)
                        - Math.floor(y / 100)
                        + Math.floor(y / 400)
                        - 32045;

        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);
        double jd = jdn + ((hour - 12) / 24) + minutes / 1440 + seconds / 86400;

        return jd;
    }
}
