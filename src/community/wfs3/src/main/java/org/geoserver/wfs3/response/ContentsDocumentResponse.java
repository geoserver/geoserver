/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.Operation;
import org.geoserver.wfs3.ContentsDocument;

/**
 * JSON/YAML encoding for the API document
 */
public class ContentsDocumentResponse extends JacksonResponse {

    public ContentsDocumentResponse(GeoServer gs) {
        super(gs, ContentsDocument.class);
    }

    protected String getFileName(Object value, Operation operation) {
        return "contents";
    }
}
