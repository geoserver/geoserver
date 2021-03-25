/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.cfg.LoadingConfiguration;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** @author Juan Marin, OpenGeo */
public class JSONValidator {
    static final Logger LOGGER = Logging.getLogger(JSONValidator.class);

    public static boolean isValidSchema(String json, File schemaFile) {
        boolean isValid = false;
        final String baseURI =
                "file:///" + schemaFile.getAbsolutePath().replace(File.separator, "/");

        JsonSchema schema;
        try {
            JsonSchemaFactory factory =
                    JsonSchemaFactory.newBuilder()
                            .setLoadingConfiguration(
                                    LoadingConfiguration.newBuilder()
                                            .setNamespace(baseURI)
                                            .freeze())
                            .freeze();
            JsonNode rawSchema = JsonLoader.fromFile(schemaFile);
            schema = factory.getJsonSchema(rawSchema);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON Schema from " + baseURI, e);
        }

        JsonNode jsonNode;
        try {
            Reader reader = new StringReader(json);
            jsonNode = JsonLoader.fromReader(reader);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't load (" + json + ") as JSON", e);
        }

        ProcessingReport report;
        try {
            report = schema.validate(jsonNode);
        } catch (ProcessingException e) {
            LOGGER.log(Level.WARNING, "", e);
            return false;
        }

        isValid = report.isSuccess();
        if (!isValid) {
            System.out.println("ERROR validating Json Schema in " + schemaFile);
            for (ProcessingMessage msg : report) {
                System.out.println(msg);
            }
        }
        return isValid;
    }
}
