/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.builders.jsonld;

import com.fasterxml.jackson.databind.JsonNode;
import org.geoserver.wfstemplating.builders.geojson.GeoJsonRootBuilder;

/** JsonLd root builder used to hold the json-ld context */
public class JsonLdRootBuilder extends GeoJsonRootBuilder {

    protected JsonNode contextHeader;

    public JsonNode getContextHeader() {
        return contextHeader;
    }

    public void setContextHeader(JsonNode contextHeader) {
        this.contextHeader = contextHeader;
    }
}
