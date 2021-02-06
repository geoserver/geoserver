/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.ows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.test.GeoServerTestSupport;

/**
 * Test class for testing instances of {@link KvpRequestReader}.
 *
 * <p>The {@link #parseKvp(Map)} method of this class sets up a kvp map and parses it by processing
 * instances of {@link KvpParser} in the application context.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class KvpRequestReaderTestSupport extends GeoServerTestSupport {
    /**
     * Parses a raw set of kvp's into a parsed set of kvps.
     *
     * @param kvp Map of String,String.
     */
    protected Map<String, Object> parseKvp(Map<String, Object> raw) throws Exception {

        // parse like the dispatcher but make sure we don't change the original map
        Map<String, Object> input = new HashMap<>(raw);
        List<Throwable> errors = KvpUtils.parse(input);
        if (errors != null && !errors.isEmpty()) throw (Exception) errors.get(0);

        return caseInsensitiveKvp(input);
    }

    protected <T> Map<String, Object> caseInsensitiveKvp(Map<String, Object> input) {
        // make it case insensitive like the servlet+dispatcher maps
        Map<String, Object> result = new HashMap<>();
        for (String key : input.keySet()) {
            result.put(key.toUpperCase(), input.get(key));
        }
        return new CaseInsensitiveMap<>(result);
    }
}
