package org.opengeo.gsr.validation;

import java.io.File;

public class JSONSchemaUtils {

    protected boolean validateJSON(String json, String schemaPath) {
        String workingDir = System.getProperty("user.dir") + "/src/test/resources/schemas/";
        return JSONValidator.isValidSchema(json, new File(workingDir + schemaPath));
    }
}
