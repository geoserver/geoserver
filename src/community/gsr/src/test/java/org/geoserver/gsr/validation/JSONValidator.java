/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.validation;

import com.networknt.schema.*;
import com.networknt.schema.Error;
import com.networknt.schema.resource.IriResourceLoader;
import java.io.*;
import java.util.List;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/** @author Juan Marin, OpenGeo */
public class JSONValidator {
    static final Logger LOGGER = Logging.getLogger(JSONValidator.class);

    public static boolean isValidSchema(String json, File schemaFile) {
        final String baseURI = "file://" + schemaFile.getAbsolutePath().replace(File.separator, "/");

        Schema schema;
        try {
            SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(
                    SpecificationVersion.DRAFT_2020_12,
                    builder -> builder.resourceLoaders(loaders -> loaders.add(new IriResourceLoader())));
            schema = schemaRegistry.getSchema(SchemaLocation.of(baseURI));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON Schema from " + baseURI, e);
        }

        List<Error> errors = schema.validate(json, InputFormat.JSON);

        boolean isValid = errors.isEmpty();
        if (!isValid) {
            System.err.println("ERROR validating JSON Schema in " + schemaFile);
            for (Error error : errors) {
                System.err.println(error);
            }
        }
        return isValid;
    }
}
