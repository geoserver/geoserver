package org.opengeo.gsr;

import java.io.File;

import org.opengeo.gsr.core.format.GeoServicesJsonFormat;
import org.opengeo.gsr.validation.JSONValidator;

import com.thoughtworks.xstream.XStream;

public abstract class JsonSchemaTest {

    final protected static XStream xstream = new GeoServicesJsonFormat().getXStream();

    public JsonSchemaTest() {
    }

    public static String getJson(Object obj) {
        return xstream.toXML(obj);
    }

    protected boolean validateJSON(String json, String schemaPath) {
        String workingDir = System.getProperty("user.dir") + "/src/test/resources/schemas/";
        return JSONValidator.isValidSchema(json, new File(workingDir + schemaPath));
    }
}
