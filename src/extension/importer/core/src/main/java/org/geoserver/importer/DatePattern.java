/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulates a date format and regular expression.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class DatePattern implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    final String format;
    final String regex;
    final boolean strict;
    final boolean forceGmt;

    Pattern pattern;

    /**
     * Constructor with defaults, <tt>forceGmt</tt> set to <tt>true</tt> and <tt>strict</tt> set to
     * <tt>false</tt>.
     */
    public DatePattern(String format, String regex) {
        this(format, regex, true, false);
    }

    /**
     * Constructor.
     *
     * @param format The date format
     * @param regex The regular expression to pull the date out of a another string.
     * @param forceGmt Whether the pattern should assume the GMT time zone.
     * @param strict Whether or not this pattern must apply the regular expression to match before
     *     parsing a date.
     */
    public DatePattern(String format, String regex, boolean forceGmt, boolean strict) {
        this.format = format;
        this.regex = regex;
        this.forceGmt = forceGmt;
        this.strict = strict;
    }

    public String getFormat() {
        return format;
    }

    public SimpleDateFormat dateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format, Locale.CANADA);
        if (forceGmt) {
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        }
        return dateFormat;
    }

    public String getRegex() {
        return regex;
    }

    public Pattern pattern() {
        if (pattern == null) {
            // wrap the regex in a group and match anything around it (use reluctant wildcard
            // matching)
            pattern = Pattern.compile(".*?(" + regex + ").*?", Pattern.CASE_INSENSITIVE);
        }
        return pattern;
    }

    /** When true the {@link #matchAndParse(String)} method should be used. */
    public boolean isStrict() {
        return strict;
    }

    public Date matchAndParse(String str) {
        Matcher m = pattern().matcher(str);
        if (!m.matches()) {
            return null;
        }

        str = m.group(1);
        return doParse(str);
    }

    public Date parse(String str) {
        if (isStrict()) {
            // matchAndParse should be called
            return null;
        }

        return doParse(str);
    }

    Date doParse(String str) {
        /*
         * We do not use the standard method DateFormat.parse(String), because
         * if the parsing stops before the end of the string, the remaining
         * characters are just ignored and no exception is thrown. So we have to
         * ensure that the whole string is correct for the format.
         */
        ParsePosition pos = new ParsePosition(0);
        Date p = dateFormat().parse(str, pos);
        if (p != null && pos.getIndex() == str.length()) {
            return p;
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (forceGmt ? 1231 : 1237);
        result = prime * result + ((format == null) ? 0 : format.hashCode());
        result = prime * result + ((regex == null) ? 0 : regex.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        DatePattern other = (DatePattern) obj;
        if (forceGmt != other.forceGmt) return false;
        if (format == null) {
            if (other.format != null) return false;
        } else if (!format.equals(other.format)) return false;
        if (regex == null) {
            if (other.regex != null) return false;
        } else if (!regex.equals(other.regex)) return false;
        return true;
    }
}
