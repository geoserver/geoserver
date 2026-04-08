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
import java.io.IOException;
import java.io.OutputStream;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** JSON converter using jackson. We mostly use spring's built in Jackson support. This persists for legacy reasons. */
@Component
public class GeoServicesJacksonJsonConverter {

    ObjectMapper mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void writeToOutputStream(OutputStream os, Object o) throws IOException {
        mapper.writeValue(os, o);
    }
}
