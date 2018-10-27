/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import org.geoserver.config.GeoServer;
import org.geotools.xsd.Configuration;

/**
 * Parses a {@code FILTER} parameter assuming the filters sent are encoded as per the OGC Filter
 * Encoding v1.0.0 specification.
 *
 * <p>This kvp parser is meant to be configured in the spring context to parse filters when a
 * GetFeature request is sent conforming to the WFS 1.0 spec.
 *
 * @author Gabriel Roldan
 */
public class Filter_1_0_0_KvpParser extends FilterKvpParser {

    public Filter_1_0_0_KvpParser(GeoServer geoServer) {
        super(geoServer);
    }

    /**
     * Provides the 1.0 filter configuration for the superclass to perform the parameter parsing.
     */
    @Override
    protected Configuration getParserConfiguration() {
        return new org.geotools.filter.v1_0.OGCConfiguration();
    }
}
