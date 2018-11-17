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
 * Encoding v2.0.0 specification.
 *
 * <p>This kvp parser is meant to be configured in the spring context to parse filters when a
 * GetFeature request is sent conforming to the WFS 2.0 spec.
 *
 * @author Justin Deoliveira
 */
public class Filter_2_0_0_KvpParser extends FilterKvpParser {

    public Filter_2_0_0_KvpParser(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    protected Configuration getParserConfiguration() {
        return new org.geotools.filter.v2_0.FESConfiguration();
    }
}
