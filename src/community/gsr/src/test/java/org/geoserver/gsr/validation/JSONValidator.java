/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.validation;

import java.io.File;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** @author Juan Marin, OpenGeo */
public class JSONValidator {
    static final Logger LOGGER = Logging.getLogger(JSONValidator.class);

    public static boolean isValidSchema(String json, File schemaFile) {
        //        boolean isValid = false;
        //        final String baseURI = "file:///" + schemaFile.getAbsolutePath().replace(File.separator, "/");
        //
        //        JsonSchema schema;
        //        try {
        //            JsonSchemaFactory factory = JsonSchemaFactory.newBuilder()
        //                    .setLoadingConfiguration(LoadingConfiguration.newBuilder()
        //                            .setNamespace(baseURI)
        //                            .freeze())
        //                    .freeze();
        //            JsonNode rawSchema = JsonLoader.fromFile(schemaFile);
        //            schema = factory.getJsonSchema(rawSchema);
        //        } catch (Exception e) {
        //            throw new RuntimeException("Failed to load JSON Schema from " + baseURI, e);
        //        }
        //
        //        JsonNode jsonNode;
        //        try {
        //            Reader reader = new StringReader(json);
        //            jsonNode = JsonLoader.fromReader(reader);
        //        } catch (Exception e) {
        //            throw new RuntimeException("Couldn't load (" + json + ") as JSON", e);
        //        }
        //
        //        ProcessingReport report;
        //        try {
        //            report = schema.validate(jsonNode);
        //        } catch (ProcessingException e) {
        //            LOGGER.log(Level.WARNING, "", e);
        //            return false;
        //        }
        //
        //        isValid = report.isSuccess();
        //        if (!isValid) {
        //            System.out.println("ERROR validating Json Schema in " + schemaFile);
        //            for (ProcessingMessage msg : report) {
        //                System.out.println(msg);
        //            }
        //        }
        //        return isValid;
        throw new RuntimeException(
                "Please migrate code to another JSON schema validator that's either using Jackson 3, or is independent of Jackson.");
    }
}
