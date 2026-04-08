/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Maps a JSON execution request from the OGC API Processes specification to a Java object. See example at
 * https://schemas.opengis.net/ogcapi/processes/part1/1.0/examples/json/Execute.json
 */
public class ExecuteRequest {

    enum ResponseMode {
        @JsonProperty("raw")
        RAW,
        @JsonProperty("document")
        DOCUMENT,
    }

    @JsonDeserialize(contentUsing = InputValueDeserializer.class)
    Map<String, InputValue> inputs;

    Map<String, ExecuteOutput> outputs;
    ResponseMode response = ResponseMode.RAW; // the default value, if not specified, is RAW (from spec)

    public Map<String, InputValue> getInputs() {
        return inputs;
    }

    public Map<String, ExecuteOutput> getOutputs() {
        return outputs;
    }

    public ResponseMode getResponse() {
        return response;
    }
}
