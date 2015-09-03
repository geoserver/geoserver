/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xml.xs;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.geotools.xml.impl.DatatypeConverterImpl;
import org.geotools.xs.bindings.XSDateBinding;

/**
 * Override of binding for xs:date that forces date to be encoded in UTC
 * timezone.
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @author awaterme
 */
public class DateBinding extends XSDateBinding {
    public String encode(Object object, String value) throws Exception {
    
        /*
         * GEOS-1235, GEOS-6777, GEOS-3651, GEOS-7053:
         * The string conversion is implementation by
         * org.geotools.xml.impl.XsDateTimeFormat#format(Object, StringBuffer,
         * java.text.FieldPosition): Apparently the CITE tests require a UTC
         * Timezone. To avoid flipping into the previous or next day, depending
         * on the system default time zone, consider the offset.
         */
        Date date = (Date) object;
        Calendar calendar = Calendar.getInstance();
        long sourceMs = date.getTime();
        int offsetMs = calendar.getTimeZone().getOffset(date.getTime());
        calendar.setTimeInMillis(sourceMs+offsetMs);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        return DatatypeConverterImpl.getInstance().printDate(calendar);
    }
}
