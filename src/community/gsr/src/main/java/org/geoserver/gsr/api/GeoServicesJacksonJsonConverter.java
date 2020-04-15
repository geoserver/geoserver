/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.OutputStream;
import org.springframework.stereotype.Component;

/**
 * JSON converter using jackson. We mostly use spring's built in Jackson support. This persists for
 * legacy reasons.
 */
@Component
public class GeoServicesJacksonJsonConverter {

    ObjectMapper mapper = new ObjectMapper();

    public GeoServicesJacksonJsonConverter() {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.getFactory().disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void writeToOutputStream(OutputStream os, Object o) throws IOException {
        mapper.writeValue(os, o);
    }
}
