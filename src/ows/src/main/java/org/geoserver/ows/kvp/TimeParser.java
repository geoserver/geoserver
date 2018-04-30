/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geotools.util.DateRange;
import org.geotools.util.logging.Logging;


/**
 * Parses the {@code time} parameter of the request. The date, time and period
 * are expected to be formatted according ISO-8601 standard.
 *
 * @author Cedric Briancon
 * @author Martin Desruisseaux
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Jonathan Meyer, Applied Information Sciences, jon@gisjedi.com
 * @version $Id$
 */
public class TimeParser {
    static final Logger LOGGER = Logging.getLogger(TimeParser.class);
    private final Integer maxTimes;

    private static enum FormatAndPrecision {
        MILLISECOND("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Calendar.MILLISECOND),
        SECOND("yyyy-MM-dd'T'HH:mm:ss'Z'", Calendar.SECOND),
        MINUTE("yyyy-MM-dd'T'HH:mm'Z'", Calendar.MINUTE),
        HOUR("yyyy-MM-dd'T'HH'Z'", Calendar.HOUR_OF_DAY),
        DAY("yyyy-MM-dd", Calendar.DAY_OF_MONTH),
        MONTH("yyyy-MM", Calendar.MONTH),
        YEAR("yyyy", Calendar.YEAR);

        public final String format;
        public final int precision;

        FormatAndPrecision(final String format, int precision) {
            this.format = format;
            this.precision = precision;
        }

        public SimpleDateFormat getFormat() {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            sdf.setTimeZone(UTC_TZ);
            return sdf;
        }

        public DateRange expand(Date d) {
            Calendar c = new GregorianCalendar(UTC_TZ);
            c.setTime(d);
            c.add(this.precision, 1);
            c.add(Calendar.MILLISECOND, -1);
            return new DateRange(d, c.getTime());
        }
    }

    /**
     * UTC timezone to serve as reference
     */
    static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    /**
     * pattern used to match back parameter
     */
	private static final Pattern pattern = Pattern.compile("(back)(\\d+)([hdw])");
    
    /**
     * Amount of milliseconds in a day.
     */
    static final long MILLIS_IN_DAY = 24*60*60*1000;

    private final static int DEFAULT_MAX_ELEMENTS_TIMES_KVP = 100;

    /**
     * Builds a default TimeParser with no provided maximum number of times
     */
    public TimeParser() {
        this.maxTimes = null;
    }

    /**
     * Parses times throwing an exception if the final list exceeds maxTimes
     *
     * @param maxTimes Maximum number of times to parse, or a non positive number to have
     *     no limit
     */
    public TimeParser(int maxTimes) {
        this.maxTimes = maxTimes;
    }
    
    /**
     * Parses the date given in parameter. The date format should comply to
     * ISO-8601 standard. The string may contains either a single date, or
     * a start time, end time and a period. In the first case, this method
     * returns a singleton containing only the parsed date. In the second
     * case, this method returns a list including all dates from start time
     * up to the end time with the interval specified in the {@code value}
     * string.
     *
     * @param value The date, time and period to parse.
     * @return A list of dates, or an empty list of the {@code value} string
     *         is null or empty.
     * @throws ParseException if the string can not be parsed.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection parse(String value) throws ParseException {
        if (value == null) {
            return Collections.emptyList();
        }
        value = value.trim();
        if (value.length() == 0) {
            return Collections.emptyList();
        }
        
        final Set result = new TreeSet(new Comparator() {

            public int compare(Object o1, Object o2) {
                final boolean o1Date= o1 instanceof Date;
                final boolean o2Date= o2 instanceof Date;
                
                if(o1 == o2) {
                    return 0;
                }
                
                // o1 date
                if(o1Date){
                    final Date dateLeft=(Date) o1;
                    if(o2Date){
                        // o2 date
                        return dateLeft.compareTo((Date) o2);
                    } 
                    // o2 daterange
                    return dateLeft.compareTo(((DateRange)o2).getMinValue());
                }
                
                // o1 date range
                final DateRange left= (DateRange) o1;
                if(o2Date){
                    // o2 date
                    return left.getMinValue().compareTo(((Date) o2));
                } 
                // o2 daterange
                return left.getMinValue().compareTo(((DateRange)o2).getMinValue());
            }
        });
        String[] listDates = value.split(",");
        int maxValues = getMaxTimes();
        for(String date: listDates){
            // is it a date or a period?
            if(date.indexOf("/")<=0){
                Object o = getFuzzyDate(date);
                if (o instanceof Date) {
                    addDate(result, (Date)o);
                } else {
                    addPeriod(result, (DateRange)o);
                }
            } else {
                // period
                String[] period = date.split("/");

                //
                // Period like one of the following: 
                // yyyy-MM-ddTHH:mm:ssZ/yyyy-MM-ddTHH:mm:ssZ/P1D
                // May be one of the following possible ISO 8601 Time Interval formats with trailing period for 
                // breaking the interval by given period:
                // TIME/TIME/PERIOD
                // DURATION/TIME/PERIOD
                // TIME/DURATION/PERIOD
                //
                if (period.length == 3) {
                    Date[] range = parseTimeDuration(period);
                    
                    final long millisIncrement = parsePeriod(period[2]);
                    final long startTime = range[0].getTime();
                    final long endTime = range[1].getTime();
                    long time;
                    int j = 0;
                    while ((time = j * millisIncrement + startTime) <= endTime) {
                        final Calendar calendar = new GregorianCalendar(UTC_TZ);
                        calendar.setTimeInMillis(time);
                        addDate(result, calendar.getTime());
                        j++;
                        checkMaxTimes(result, maxValues);
                    }
                } 
                // Period like : yyyy-MM-ddTHH:mm:ssZ/yyyy-MM-ddTHH:mm:ssZ, it is an extension 
                // of WMS that works with continuos period [Tb, Te].
                // May be one of the following possible ISO 8601 Time Interval formats, as in ECQL Time Period:
                // TIME/DURATION
                // DURATION/TIME
                // TIME/TIME
                else if (period.length == 2) {
                        Date[] range = parseTimeDuration(period);
                        addPeriod(result, new DateRange(range[0], range[1]));
                } else {
                    throw new ParseException("Invalid time period: " + Arrays.toString(period), 0);
                }
            }
            checkMaxTimes(result, maxValues);
        }
        
        return new ArrayList(result);
    }

    /**
     * Maximum number of times this parser will parse before throwing an exception
     * @return
     */
    private int getMaxTimes() {
        if (maxTimes != null) {
            return maxTimes;
        } else {
            return DEFAULT_MAX_ELEMENTS_TIMES_KVP;
        }
    }

    public void checkMaxTimes(Set result, int maxValues) {
        // limiting number of elements we can create
        if(maxValues > 0 && result.size() > maxValues){
            throw new ServiceException("More than " + maxValues
                    + " times specified in the request, bailing out.", ServiceException.INVALID_PARAMETER_VALUE, "time");              
        }
    }

    private static Date[] parseTimeDuration(final String[] period) throws ParseException {
        Date[] range = null;

        if (period.length == 2 || period.length == 3) {
            Date begin = null;
            Date end = null;

            // Check first to see if we have any duration value within TIME parameter
            if (period[0].toUpperCase().startsWith("P") || period[1].toUpperCase().startsWith("P")) {
                long durationOffset = Long.MIN_VALUE;

                // Attempt to parse a time or duration from the first portion of the
                if (period[0].toUpperCase().startsWith("P")) {
                    durationOffset = parsePeriod(period[0]);
                } else {
                    begin = beginning(getFuzzyDate(period[0]));
                }

                if (period[1].toUpperCase().startsWith("P")
                        && !period[1].toUpperCase().startsWith("PRESENT")) {
                    // Invalid time period of the format:
                    // DURATION/DURATION[/PERIOD]
                    if (durationOffset != Long.MIN_VALUE) {
                        throw new ParseException(
                                "Invalid time period containing duration with no paired time value: "
                                        + Arrays.toString(period), 0);
                    }
                    // Time period of the format:
                    // DURATION/TIME[/PERIOD]
                    else {
                        durationOffset = parsePeriod(period[1]);
                        final Calendar calendar = new GregorianCalendar();
                        calendar.setTimeInMillis(begin.getTime() + durationOffset);
                        end = calendar.getTime();
                    }
                }
                // Time period of the format:
                // TIME/DURATION[/PERIOD]
                else {
                    end = end(getFuzzyDate(period[1]));
                    final Calendar calendar = new GregorianCalendar();
                    calendar.setTimeInMillis(end.getTime() - durationOffset);
                    begin = calendar.getTime();
                }
            }
            // Time period of the format:
            // TIME/TIME[/PERIOD]
            else {
                begin = beginning(getFuzzyDate(period[0]));
                end = end(getFuzzyDate(period[1]));
            }

            range = new Date[2];
            range[0] = begin;
            range[1] = end;

        }

        return range;
    }

    private static Date beginning(Object dateOrDateRange) {
        if (dateOrDateRange instanceof DateRange) {
            return ((DateRange) dateOrDateRange).getMinValue();
        } else {
            return (Date) dateOrDateRange;
        }
    }
    
    private static Date end(Object dateOrDateRange) {
        if (dateOrDateRange instanceof DateRange) {
            return ((DateRange) dateOrDateRange).getMaxValue();
        } else {
            return (Date) dateOrDateRange;
        }
    }
    
    /**
     * Tries to avoid insertion of multiple time values.
     * 
     * @param result
     * @param newRange
     */
    private static void addPeriod(Collection result, DateRange newRange) {
        for(Iterator it=result.iterator();it.hasNext();){
            final Object element=it.next();
            if(element instanceof Date){
                // convert
                final Date local= (Date) element;
                if(newRange.contains(local)){
                    it.remove();
                }
            } else {
                // convert
                final DateRange local= (DateRange) element;
                if(local.contains(newRange))
                    return;
                if(newRange.contains(local))
                    it.remove();
            }
        }
        result.add(newRange);
    }
    
    private static void addDate(Collection result, Date newDate) {
        for (Iterator<?> it = result.iterator(); it.hasNext(); ) {
            final Object element = it.next();
            if (element instanceof Date) {
                if (newDate.equals(element)) return;
            } else if (((DateRange) element).contains(newDate)) {
                return;
            }
        }
        result.add(newDate);
    }

    /**
     * Parses date given in parameter according the ISO-8601 standard. This parameter should follow 
     * a syntax defined in the {@link #PATTERNS} array to be validated.
     *
     * @param value The date to parse.
     * @return A date found in the request.
     * @throws ParseException if the string can not be parsed.
     */
    static Object getFuzzyDate(final String value) throws ParseException {
        String computedValue = value;

        // special handling for current keyword (we accept both wms and wcs ways)
        if (computedValue.equalsIgnoreCase("current") || computedValue.equalsIgnoreCase("now")) {
            return null;
        }

        // Accept new "present" keyword, which actually fills in present time as now should have
        if (computedValue.equalsIgnoreCase("present")) {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.MILLISECOND, 0);
            computedValue = FormatAndPrecision.MILLISECOND.getFormat().format(now.getTime());
        }

        for (FormatAndPrecision f : FormatAndPrecision.values()) {
            ParsePosition pos = new ParsePosition(0);
            Date time = f.getFormat().parse(computedValue, pos);
            if (pos.getIndex() == computedValue.length()) {
                DateRange range = f.expand(time);
                if (range.getMinValue().equals(range.getMaxValue())) {
                    return range.getMinValue();
                } else {
                    return range;
                }
            }
        }

        throw new ParseException("Invalid date: " + value, 0);
    }

    /**
     * Parses the increment part of a period and returns it in milliseconds.
     *
     * @param period A string representation of the time increment according the ISO-8601:1988(E)
     *        standard. For example: {@code "P1D"} = one day.
     * @return The increment value converted in milliseconds.
     * @throws ParseException if the string can not be parsed.
     */
    public static long parsePeriod(final String period) throws ParseException {
        final int length = period.length();
        if (length!=0 && Character.toUpperCase(period.charAt(0)) != 'P') {
            throw new ParseException("Invalid period increment given: " + period, 0);
        }
        long millis = 0;
        boolean time = false;
        int lower = 0;
        while (++lower < length) {
            char letter = Character.toUpperCase(period.charAt(lower));
            if (letter == 'T') {
                time = true;
                if (++lower >= length) {
                    break;
                }
            }
            int upper = lower;
            letter = period.charAt(upper);
            while (!Character.isLetter(letter) || letter == 'e' || letter == 'E') {
                if (++upper >= length) {
                    throw new ParseException("Missing symbol in \"" + period + "\".", lower);
                }
                letter = period.charAt(upper);
            }
            letter = Character.toUpperCase(letter);
            final double value = Double.parseDouble(period.substring(lower, upper));
            final double factor;
            if (time) {
                switch (letter) {
                    case 'S': factor =       1000; break;
                    case 'M': factor =    60*1000; break;
                    case 'H': factor = 60*60*1000; break;
                    default: throw new ParseException("Unknown time symbol: " + letter, upper);
                }
            } else {
                switch (letter) {
                    case 'D': factor =               MILLIS_IN_DAY; break;
                    case 'W': factor =           7 * MILLIS_IN_DAY; break;
                    // TODO: handle months in a better way than just taking the average length.
                    case 'M': factor =          30 * MILLIS_IN_DAY; break;
                    case 'Y': factor =      365.25 * MILLIS_IN_DAY; break;
                    default: throw new ParseException("Unknown period symbol: " + letter, upper);
                }
            }
            millis += Math.round(value * factor);
            lower = upper;
        }
        return millis;
    }
}
