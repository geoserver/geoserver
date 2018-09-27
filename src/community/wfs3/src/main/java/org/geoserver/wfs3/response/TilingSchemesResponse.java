package org.geoserver.wfs3.response;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;

public class TilingSchemesResponse extends JacksonResponse {

    public TilingSchemesResponse(GeoServer gs) {
        super(gs, TilingSchemesDocument.class);
    }

    @Override
    protected String getFileName(Object value, Operation operation) {
        return "tilingSchemes";
    }
}
