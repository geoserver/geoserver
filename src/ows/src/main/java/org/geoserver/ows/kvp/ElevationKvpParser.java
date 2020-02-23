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
 * Parses the {@code elevation} parameter of the request.
 *
 * @author Ariel Nunez, GeoSolutions S.A.S.
 * @author Simone Giannecchini, GeoSolutions S.A.S.
 * @version $Id$
 */
public class ElevationKvpParser extends KvpParser {

    ElevationParser parser = new ElevationParser();

    /**
     * Creates the parser specifying the name of the key to latch to.
     *
     * @param key The key whose associated value to parse.
     */
    public ElevationKvpParser(String key) {
        super(key, List.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object parse(String value) throws ParseException {
        ElevationParser parser = getElevationParser();
        return parser.parse(value);
    }

    /** Allows subclasses to customize the {@link ElevationParser} used in {@link #parse(String)} */
    protected ElevationParser getElevationParser() {
        return parser;
    }
}
