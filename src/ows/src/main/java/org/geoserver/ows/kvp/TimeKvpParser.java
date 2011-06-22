/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.logging.Level;

import org.geoserver.ows.KvpParser;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.DateRange;


/**
 * Parses the {@code time} parameter of the request. The date, time and period
 * are expected to be formatted according ISO-8601 standard.
 *
 * @author Cedric Briancon
 * @author Martin Desruisseaux
 * @author Simone Giannecchini, GeoSolutions SAS
 * @version $Id$
 */
public class TimeKvpParser extends KvpParser {
    /**
     * All patterns that are correct regarding the ISO-8601 norm.
     */
    private static final String[] PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:sss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm'Z'",
        "yyyy-MM-dd'T'HH'Z'",
        "yyyy-MM-dd",
        "yyyy-MM",
        "yyyy"
    };
    
    /**
     * UTC timezone to serve as reference
     */
    static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    /**
     * Amount of milliseconds in a day.
     */
    static final long MILLIS_IN_DAY = 24*60*60*1000;


    /**
     * Built-in limits to avoid exploding on too large requests
     */
    private final static int MAX_ELEMENTS_TIMES_KVP;
    private final static int DEFAULT_MAX_ELEMENTS_TIMES_KVP = 100;

    static {
        // initialization of the renderer choice flag
        String value = GeoServerExtensions.getProperty("MAX_ELEMENTS_TIMES_KVP");
        // default to true, but allow switching on
        if (value == null)
            MAX_ELEMENTS_TIMES_KVP = DEFAULT_MAX_ELEMENTS_TIMES_KVP;
        else {
            int iVal = -1;
            try {
                iVal = Integer.parseInt(value.trim());
            } catch (Exception e) {
                iVal = DEFAULT_MAX_ELEMENTS_TIMES_KVP;
            }
            MAX_ELEMENTS_TIMES_KVP = iVal;
        }
    }
    
    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public TimeKvpParser(String key) {
        super(key, List.class);
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
    public Object parse(String value) throws ParseException {
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
        for(String date: listDates){
            // is it a date or a period?
            if(date.indexOf("/")<=0){
                addDate(result,getDate(date));
            } else {
                // period
                String[] period = date.split("/");
                //
                // Period like : yyyy-MM-ddTHH:mm:ssZ/yyyy-MM-ddTHH:mm:ssZ/P1D
                //
                if (period.length == 3) {
                    final Date begin = getDate(period[0]);
                    final Date end   = getDate(period[1]);
                    final long millisIncrement = parsePeriod(period[2]);
                    final long startTime = begin.getTime();
                    final long endTime = end.getTime();
                    long time;
                    int j = 0;
                    while ((time = j * millisIncrement + startTime) <= endTime) {
                        final Calendar calendar = Calendar.getInstance(UTC_TZ);
                        calendar.setTimeInMillis(time);
                        addDate(result,calendar.getTime());
                        j++;
                        
                        // limiting number of elements we can create
                        if(j>= MAX_ELEMENTS_TIMES_KVP){
                            if(LOGGER.isLoggable(Level.INFO))
                                LOGGER.info("Lmiting number of elements in this periodo to "+MAX_ELEMENTS_TIMES_KVP);
                            break;                  
                        }
                    }
                } else if (period.length == 2) {
                        // Period like : yyyy-MM-ddTHH:mm:ssZ/yyyy-MM-ddTHH:mm:ssZ, it is an extension 
                        // of WMS that works with continuos period [Tb, Te].
                        final Date begin = getDate(period[0]);
                        final Date end   = getDate(period[1]);
                        addPeriod(result,new DateRange(begin, end));
                } else {
                    throw new ParseException("Invalid time period: " + period, 0);
                }
            }
        }
        
        return new ArrayList(result);

    }

    /**
     * Tries to avoid insertion of multiple time values.
     * 
     * @param result
     * @param time
     */
    private static void addDate(Collection result, Date time) {
        for(Iterator it=result.iterator();it.hasNext();){
            final Object element=it.next();
            if(element instanceof Date){
                // convert
                final Date local= (Date) element;
                if(local.equals(time))
                    return;
            } else {
                // convert
                final DateRange local= (DateRange) element;
                if(local.contains(time))
                    return;
            }
        }
        result.add(time);
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

    /**
     * Parses date given in parameter according the ISO-8601 standard. This parameter
     * should follow a syntax defined in the {@link #PATTERNS} array to be validated.
     *
     * @param value The date to parse.
     * @return A date found in the request.
     * @throws ParseException if the string can not be parsed.
     */
    private static Date getDate(final String value) throws ParseException {
    	
    	// special handling for current keyword (we accept both wms and wcs ways)
    	if(value.equalsIgnoreCase("current") || value.equalsIgnoreCase("now")) {
    		return null;
    	}
        for (int i=0; i<PATTERNS.length; i++) {
            // rebuild formats at each parse, date formats are not thread safe
            SimpleDateFormat format = new SimpleDateFormat(PATTERNS[i], Locale.CANADA);
            format.setTimeZone(UTC_TZ);
            /* We do not use the standard method DateFormat.parse(String), because if the parsing
             * stops before the end of the string, the remaining characters are just ignored and
             * no exception is thrown. So we have to ensure that the whole string is correct for
             * the format.
             */
            ParsePosition pos = new ParsePosition(0);
            Date time = format.parse(value, pos);
            if (pos.getIndex() == value.length()) {
                return time;
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
    static long parsePeriod(final String period) throws ParseException {
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