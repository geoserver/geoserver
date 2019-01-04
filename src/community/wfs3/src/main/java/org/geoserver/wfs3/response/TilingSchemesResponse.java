/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;

/** Encoder for the tiling schemes */
public class TilingSchemesResponse extends JacksonResponse {

    public TilingSchemesResponse(GeoServer gs) {
        super(gs, TilingSchemesDocument.class);
    }

    @Override
    protected String getFileName(Object value, Operation operation) {
        return "tilingSchemes";
    }
}
