/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.validation;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;

import org.eel.kitchen.jsonschema.main.JsonSchema;
import org.eel.kitchen.jsonschema.main.JsonSchemaFactory;
import org.eel.kitchen.jsonschema.main.SchemaContainer;
import org.eel.kitchen.jsonschema.main.ValidationReport;
import org.eel.kitchen.jsonschema.util.JsonLoader;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class JSONValidator {

    public static boolean isValidSchema(String json, File schemaFile) {
        boolean isValid = false;
        try {
            final String baseURI = "file:///" + schemaFile.getAbsolutePath();
            JsonSchemaFactory factory = new JsonSchemaFactory.Builder().setNamespace(baseURI)
                    .build();
            JsonNode rawSchema = JsonLoader.fromFile(schemaFile);
            SchemaContainer schemaContainer = factory.registerSchema(rawSchema);
            JsonSchema schema = factory.createSchema(schemaContainer);
            Reader reader = new StringReader(json);
            JsonNode jsonNode = JsonLoader.fromReader(reader);
            ValidationReport report = schema.validate(jsonNode);
            isValid = report.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return isValid;
    }
}
