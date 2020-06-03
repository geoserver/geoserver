/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import net.sf.json.JSON;
import net.sf.json.JSONSerializer;
import org.geoserver.gsr.api.GeoServicesJacksonJsonConverter;
import org.geoserver.gsr.validation.JSONValidator;

public abstract class JsonSchemaTest {

    protected static final ObjectMapper mapper = new GeoServicesJacksonJsonConverter().getMapper();

    public JsonSchemaTest() {}

    public static String getJson(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }

    public static boolean validateJSON(String json, String schemaPath) {
        return validateJSON(JSONSerializer.toJSON(json), schemaPath);
    }

    public static boolean validateJSON(JSON json, String schemaPath) {
        File schemaFile =
                new java.io.File(
                        System.getProperty("user.dir")
                                + "/src/test/resources/schemas/"
                                + schemaPath);
        return JSONValidator.isValidSchema(json.toString(), schemaFile);
    }
}
