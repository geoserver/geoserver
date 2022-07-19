/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.List;

/** Format parser interface for view parameters. */
public interface ViewParamsFormatParser {

    /** Returns the string identifier for this view params parser. */
    String getIdentifier();

    /**
     * Parses the provided string into a list of values.
     *
     * @param value the string value to parse
     * @return a list of parsed parameters
     * @throws Exception
     */
    List<Object> parse(String value) throws Exception;
}
