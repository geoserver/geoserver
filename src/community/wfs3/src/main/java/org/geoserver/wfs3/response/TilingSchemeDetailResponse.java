package org.geoserver.wfs3.response;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;

public class TilingSchemeDetailResponse extends JacksonResponse {

    public TilingSchemeDetailResponse(GeoServer gs) {
        super(gs, TilingSchemeDescriptionDocument.class);
    }

    @Override
    protected String getFileName(Object value, Operation operation) {
        return "tilingSchemeDetail";
    }
}
