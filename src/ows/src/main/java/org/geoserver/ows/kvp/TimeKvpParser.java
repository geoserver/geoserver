/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.text.ParseException;
import java.util.List;

import org.geoserver.ows.KvpParser;


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
public class TimeKvpParser extends KvpParser {
    
    TimeParser parser = new TimeParser();

    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public TimeKvpParser(String key) {
        super(key, List.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Object parse(String value) throws ParseException {
        TimeParser parser = getTimeParser();
        return parser.parse(value);
    }

    /**
     * Allows subclasses to customize the {@link TimeParser} used in {@link #parse(String)}
     * @return
     */
    protected TimeParser getTimeParser() {
        return parser;
    }

}
