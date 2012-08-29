package org.opengeo.gsr.validation;

import java.io.File;

import org.geoserver.test.GeoServerTestSupport;

public class JSONSchemaUtils extends GeoServerTestSupport {

    protected boolean validateJSON(String json, String schemaPath) {
        String workingDir = System.getProperty("user.dir") + "/src/test/resources/schemas/";
        return JSONValidator.isValidSchema(json, new File(workingDir + schemaPath));
    }
}
