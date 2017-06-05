/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr;

import java.io.File;

import com.boundlessgeo.gsr.api.GeoServicesJSONConverter;
import com.boundlessgeo.gsr.validation.JSONValidator;

import com.thoughtworks.xstream.XStream;

public abstract class JsonSchemaTest {

    final protected static XStream xstream = new GeoServicesJSONConverter().getXStream();

    public JsonSchemaTest() {
    }

    public static String getJson(Object obj) {
        return xstream.toXML(obj);
    }

    public static boolean validateJSON(String json, String schemaPath) {
        File schemaFile = new java.io.File(System.getProperty("user.dir") + "/src/test/resources/schemas/" + schemaPath);
        return JSONValidator.isValidSchema(json, schemaFile);
    }
}
