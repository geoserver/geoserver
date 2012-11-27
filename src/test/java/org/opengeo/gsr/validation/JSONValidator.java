/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.validation;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;

import javax.management.RuntimeErrorException;

import org.eel.kitchen.jsonschema.main.JsonSchema;
import org.eel.kitchen.jsonschema.main.JsonSchemaFactory;
import org.eel.kitchen.jsonschema.ref.SchemaContainer;
import org.eel.kitchen.jsonschema.report.ValidationReport;
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
        ValidationReport report = null;
        final String baseURI = "file:///" + schemaFile.getAbsolutePath();
        
        try {
            JsonSchemaFactory factory = new JsonSchemaFactory.Builder().setNamespace(baseURI)
                    .build();
            JsonNode rawSchema = JsonLoader.fromFile(schemaFile);
            SchemaContainer schemaContainer = factory.registerSchema(rawSchema);
            JsonSchema schema = factory.createSchema(schemaContainer);
            Reader reader = new StringReader(json);
            JsonNode jsonNode = JsonLoader.fromReader(reader);
            report = schema.validate(jsonNode);
            isValid = report.isSuccess();
            if (!isValid) {
                System.out.println("ERROR validating Json Schema in " + schemaFile);
                for (String msg : report.getMessages()) {
                    System.out.println(msg);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load json schema from " + baseURI);
            e.printStackTrace();
            report.getMessages().add(e.getMessage());
        }
        return isValid;
    }
}
