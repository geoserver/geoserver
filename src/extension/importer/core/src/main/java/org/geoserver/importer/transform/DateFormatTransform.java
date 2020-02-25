/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.text.ParseException;
import java.util.Date;
import java.util.logging.Level;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
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

    private String enddate;

    private String presentation;

    /**
     * Default Constructor taking two parameters - [mandatory] The field used as time dimension -
     * [optional] The date-time pattern to be used in case of String fields
     */
    public DateFormatTransform(String field, String datePattern) throws ValidationException {
        this(field, datePattern, null, null);
    }

    /**
     * Default Constructor taking four parameters - [mandatory] The field used as time dimension -
     * [optional] The date-time pattern to be used in case of String fields - [optional] The field
     * used as end date for the time dimension - [optional] The time dimension presentation type;
     * one of {LIST; DISCRETE_INTERVAL; CONTINUOUS_INTERVAL}
     */
    public DateFormatTransform(
            String field, String datePattern, String enddate, String presentation)
            throws ValidationException {
        init(field, datePattern, enddate, presentation);
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

    /** @return the enddate */
    public String getEnddate() {
        return enddate;
    }

    /** @param enddate the enddate to set */
    public void setEnddate(String enddate) {
        this.enddate = enddate;
    }

    /** @return the presentation */
    public String getPresentation() {
        return presentation;
    }

    /** @param presentation the presentation to set */
    public void setPresentation(String presentation) {
        this.presentation = presentation;
    }

    private void init(String field, String datePattern, String enddate, String presentation)
            throws ValidationException {
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
        this.enddate = enddate;
        this.presentation = presentation != null ? presentation : "LIST";
    }

    @Override
    public SimpleFeature apply(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        Object val = oldFeature.getAttribute(field);
        if (val != null) {
            Date parsed = (val instanceof Date ? (Date) val : parseDate(val.toString()));
            if (parsed == null) {
                task.addMessage(
                        Level.WARNING,
                        "Invalid date '" + val + "' specified for " + feature.getID());
                feature = null;
            } else {
                feature.setAttribute(field, parsed);

                if (enddate != null) {
                    val = oldFeature.getAttribute(field);
                    if (val != null) {
                        parsed = (val instanceof Date ? (Date) val : parseDate(val.toString()));
                        if (parsed != null) {
                            feature.setAttribute(enddate, parsed);
                        }
                    }
                }
            }
        }

        // set up the time dimension object
        if (task.getLayer() != null) {
            ResourceInfo r = task.getLayer().getResource();
            if (r != null && r.getMetadata().get(ResourceInfo.TIME) == null) {
                DimensionInfo dim = new DimensionInfoImpl();
                dim.setEnabled(true);
                dim.setAttribute(field);
                dim.setEndAttribute(enddate);
                dim.setPresentation(DimensionPresentation.valueOf(presentation));
                dim.setUnits("ISO8601"); // TODO: is there an enumeration for this?

                r.getMetadata().put(ResourceInfo.TIME, dim);
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
