/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.wcs2_0.exception.WCS20Exception;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.geotools.xml.impl.DatatypeConverterImpl;

/**
 * Class to parse different types of dimension values
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class WCSDimensionsValueParser {

    private static final Logger LOGGER = Logging.getLogger(WCSDimensionsValueParser.class);

    private static final DatatypeConverterImpl XML_CONVERTER = DatatypeConverterImpl.getInstance();

    /** Parse a string value as a {@link Date} */
    public Date parseDateTime(String value) {
        return XML_CONVERTER.parseDateTime(value).getTime();
    }

    /** Parse a string value as a {@link Double} */
    public Double parseDouble(String value) {
        return XML_CONVERTER.parseDouble(value);
    }

    /** Set the slicePoint string as an {@link Integer}. Return true in case of success */
    public boolean setAsInteger(String slicePointS, List<Object> selectedValues) {
        final Integer slicePoint = parseAsInteger(slicePointS);
        if (slicePoint != null) {
            selectedValues.add(slicePoint);
            return true;
        }
        return false;
    }

    /** Set the 2 strings as an {@link Integer} range. Return true in case of success */
    public boolean setAsIntegerRange(String low, String high, List<Object> selectedValues) {
        final Integer l = parseAsInteger(low);
        final Integer h = parseAsInteger(high);
        if (l != null && h != null) {
            if (l > h) {
                throwInvalidRangeException(low, high);
            }
            selectedValues.add(new NumberRange(Integer.class, l, h));
            return true;
        }
        return false;
    }

    /** Set the slicePoint string as an {@link Double}. Return true in case of success */
    public boolean setAsDouble(String slicePointS, List<Object> selectedValues) {
        final Double slicePoint = parseAsDouble(slicePointS);
        if (slicePoint != null) {
            selectedValues.add(slicePoint);
            return true;
        }
        return false;
    }

    /** Set the 2 strings as an {@link Double} range. Return true in case of success */
    public boolean setAsDoubleRange(String low, String high, List<Object> selectedValues) {
        final Double l = parseAsDouble(low);
        final Double h = parseAsDouble(high);
        if (l != null && h != null) {
            if (l > h) {
                throwInvalidRangeException(low, high);
            }
            selectedValues.add(new NumberRange(Double.class, l, h));
            return true;
        }
        return false;
    }

    /** Set the slicePoint string as an {@link Date}. Return true in case of success */
    public boolean setAsDate(String slicePointS, List<Object> selectedValues) {
        final Date slicePoint = parseAsDate(slicePointS);
        if (slicePoint != null) {
            selectedValues.add(slicePoint);
            return true;
        }
        return false;
    }

    /** Set the 2 strings as a DateRange. Return true in case of success */
    public boolean setAsDateRange(String low, String high, List<Object> selectedValues) {
        final Date l = parseAsDate(low);
        final Date h = parseAsDate(high);
        if (l != null && h != null) {
            if (l.compareTo(h) > 0) {
                throwInvalidRangeException(low, high);
            }
            selectedValues.add(new DateRange(l, h));
            return true;
        }
        return false;
    }

    /** Parse a String as a Double or return null if impossible. */
    public static Double parseAsDouble(String text) {
        try {
            final Double slicePoint = XML_CONVERTER.parseDouble(text);
            return slicePoint;
        } catch (NumberFormatException nfe) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(text + " can't be parsed as an Double.");
            }
        }
        return null;
    }

    /** Parse a String as a Range of Double or return null if impossible. */
    public static NumberRange<Double> parseAsDoubleRange(String text) {
        try {
            if (text.contains("/")) {
                String[] range = text.split("/");
                if (range.length == 2) {
                    String min = range[0];
                    String max = range[1];
                    final Double minValue = XML_CONVERTER.parseDouble(min);
                    final Double maxValue = XML_CONVERTER.parseDouble(max);
                    return new NumberRange<Double>(Double.class, minValue, maxValue);
                }
            }
        } catch (NumberFormatException nfe) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(text + " can't be parsed as an Double.");
            }
        }
        return null;
    }

    /** Parse a String as an Integer or return null if impossible. */
    public Integer parseAsInteger(String text) {
        try {
            final Integer slicePoint = XML_CONVERTER.parseInt(text);
            return slicePoint;
        } catch (NumberFormatException nfe) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(text + " can't be parsed as an Integer.");
            }
        }
        return null;
    }

    /** Parse a String as a Date or return null if impossible. */
    public static Date parseAsDate(String text) {
        try {
            final Date slicePoint = XML_CONVERTER.parseDateTime(text).getTime();
            if (slicePoint != null) {
                return slicePoint;
            }
        } catch (IllegalArgumentException iae) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(text + " can't be parsed as a time");
            }
        }
        return null;
    }

    /** Set the slice value as proper object depending on the datatype */
    public void setValues(String slicePointS, List<Object> selectedValues, String domainDatatype) {
        if (domainDatatype.endsWith("Timestamp") || domainDatatype.endsWith("Date")) {
            setAsDate(slicePointS, selectedValues);
        } else if (domainDatatype.endsWith("Integer")) {
            setAsInteger(slicePointS, selectedValues);
        } else if (domainDatatype.endsWith("Double")) {
            setAsDouble(slicePointS, selectedValues);
        } else if (domainDatatype.endsWith("String")) {
            selectedValues.add(slicePointS);
        }
        // TODO: Add support for more datatype management
    }

    /** Set the slice value as proper object depending on the datatype */
    public void setRangeValues(
            String low, String high, List<Object> selectedValues, String domainDatatype) {
        if (domainDatatype.endsWith("Timestamp") || domainDatatype.endsWith("Date")) {
            setAsDateRange(low, high, selectedValues);
        } else if (domainDatatype.endsWith("Integer")) {
            setAsIntegerRange(low, high, selectedValues);
        } else if (domainDatatype.endsWith("Double")) {
            setAsDoubleRange(low, high, selectedValues);
        } else if (domainDatatype.endsWith("String")) {
            selectedValues.add(low + "/" + high); // TODO Check me
        }
        // TODO: Add support for more datatype management
    }

    /** Get the domain set as a set of number. */
    public TreeSet<Double> getDomainNumber(TreeSet<Object> domain) {
        TreeSet<Double> results = new TreeSet<Double>();
        for (Object item : domain) {
            if (item instanceof Number) {
                Double number = (Double) item;
                results.add(number);
            } else if (item instanceof NumberRange) {
                NumberRange range = (NumberRange) item;
                results.add(range.getMinimum());
                results.add(range.getMaximum());
            } else {
                throw new IllegalArgumentException(
                        "The specified domain set doesn't contain Number or NumberRange instances");
            }
        }
        return results;
    }

    private static void throwInvalidRangeException(String low, String high) {
        throw new WCS20Exception(
                "Low greater than High: " + low + ", " + high,
                WCS20Exception.WCS20ExceptionCode.InvalidSubsetting,
                "subset");
    }
}
