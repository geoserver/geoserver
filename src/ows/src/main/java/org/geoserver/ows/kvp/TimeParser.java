/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.Set;
import org.geoserver.platform.ServiceException;
import org.geotools.util.DateTimeParser;

/**
 * Parses the {@code time} parameter of the request. The date, time and period are expected to be
 * formatted according ISO-8601 standard.
 *
 * @author Cedric Briancon
 * @author Martin Desruisseaux
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Jonathan Meyer, Applied Information Sciences, jon@gisjedi.com
 * @version $Id$
 */
public class TimeParser extends DateTimeParser {

    private static final int DEFAULT_MAX_ELEMENTS_TIMES_KVP = 100;

    private static final int DEFAULT_FLAGS =
            FLAG_GET_TIME_ON_PRESENT | FLAG_SINGLE_DATE_AS_DATERANGE;

    /** Builds a default TimeParser with no provided maximum number of times */
    public TimeParser() {
        this(DEFAULT_MAX_ELEMENTS_TIMES_KVP);
    }

    public TimeParser(int maxTimes) {
        super(maxTimes, DEFAULT_FLAGS);
    }

    @Override
    public void checkMaxTimes(Set result, int maxValues) {
        // limiting number of elements we can create
        if (maxValues > 0 && result.size() > maxValues) {
            throw new ServiceException(
                    "More than " + maxValues + " times specified in the request, bailing out.",
                    ServiceException.INVALID_PARAMETER_VALUE,
                    "time");
        }
    }
}
