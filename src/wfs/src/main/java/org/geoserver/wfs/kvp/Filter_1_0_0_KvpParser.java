/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import org.geotools.xml.Configuration;

/**
 * Parses a {@code FILTER} parameter assuming the filters sent are encoded as
 * per the OGC Filter Encoding v1.0.0 specification.
 * <p>
 * This kvp parser is meant to be configured in the spring context to parse
 * filters when a GetFeature request is sent conforming to the WFS 1.0 spec.
 * </p>
 * 
 * @author Gabriel Roldan
 */
public class Filter_1_0_0_KvpParser extends FilterKvpParser {

    /**
     * Provides the 1.0 filter configuration for the superclass to perform the
     * parameter parsing.
     */
    @Override
    protected Configuration getParserConfiguration() {
        return new org.geotools.filter.v1_0.OGCConfiguration();
    }
}
