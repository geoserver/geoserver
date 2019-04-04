/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package org.geoserver.importer.transform;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.geoserver.importer.ImportTask;
import org.geotools.data.DataStore;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Allow a string or number field to be used as a year Date. The number is interpreted as an
 * Integer.
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class IntegerFieldToDateTransform extends AttributeRemapTransform {

    private static final long serialVersionUID = 1L;

    transient Calendar calendar;

    public IntegerFieldToDateTransform(String field) {
        super(field, Date.class);
    }

    public void init() {
        calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(Calendar.DAY_OF_YEAR, 1);
    }

    @Override
    public SimpleFeature apply(
            ImportTask task, DataStore dataStore, SimpleFeature oldFeature, SimpleFeature feature)
            throws Exception {
        Object val = oldFeature.getAttribute(field);
        Date parsed = null;
        if (val instanceof String) {
            String s = ((String) val).trim();
            if (s.length() > 0) {
                val = Double.parseDouble(s);
            } else {
                val = null;
            }
        }
        if (val != null) {
            parsed = parseDate((Number) val);
        }
        feature.setAttribute(field, parsed);
        return feature;
    }

    private Date parseDate(Number val) {
        calendar.set(Calendar.YEAR, val.intValue());
        return calendar.getTime();
    }
}
