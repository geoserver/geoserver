/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import org.geoserver.importer.DatePattern;
import org.geoserver.importer.Dates;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ValidationException;
import org.geotools.data.DataStore;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Transform that converts a non date attribute in a date attribute. This class is not thread-safe.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class DateFormatTransform extends AttributeRemapTransform {

    private static final long serialVersionUID = 1L;

    DatePattern datePattern;

    public DateFormatTransform(String field, String datePattern) throws ValidationException {
        init(field, datePattern);
        init();
    }

    DateFormatTransform() {
        this(null, null);
    }

    public DatePattern getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(DatePattern datePattern) {
        this.datePattern = datePattern;
    }

    private void init(String field, String datePattern) throws ValidationException {
        setType(Date.class);
        setField(field);
        if (datePattern != null) {
            this.datePattern = new DatePattern(datePattern, null, true, false);

            // parse the date format to ensure its legal
            try {
                this.datePattern.dateFormat();
            } catch (IllegalArgumentException iae) {
                throw new ValidationException("Invalid date parsing format", iae);
            }
        }
    }

    @Override
    public SimpleFeature apply(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        Object val = oldFeature.getAttribute(field);
        if (val != null) {
            Date parsed = parseDate(val.toString());
            if (parsed == null) {
                task.addMessage(
                        Level.WARNING,
                        "Invalid date '" + val + "' specified for " + feature.getID());
                feature = null;
            } else {
                feature.setAttribute(field, parsed);
            }
        }
        return feature;
    }

    public Date parseDate(String value) throws ParseException {
        Date parsed = null;

        // if a format was provided, use it
        if (datePattern != null) {
            parsed = datePattern.parse(value);
        }

        // fall back to others
        if (parsed == null) {
            parsed = Dates.parse(value);
        }
        if (parsed != null) {
            return parsed;
        }

        throw new ParseException("Invalid date '" + value + "'", 0);
    }
}
