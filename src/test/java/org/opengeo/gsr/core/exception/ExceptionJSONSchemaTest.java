/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.exception;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opengeo.gsr.JsonSchemaTest;

public class ExceptionJSONSchemaTest extends JsonSchemaTest {

    public ExceptionJSONSchemaTest() {
        super();
    }

    @Test
    public void testServiceErrorJSONSchema() throws Exception {
        List<String> details = new ArrayList<String>();
        details.add("Bad request details");
        ServiceError error = new ServiceError(400, "Bad Request", details);
        String json = getJson(error);
        assertTrue(validateJSON(json, "gsr/1.0/error.json"));
    }

    @Test
    public void testServiceExceptionJSONSchema() throws Exception {
        List<String> details = new ArrayList<String>();
        details.add("Bad request details");
        ServiceError error = new ServiceError(400, "Bad Request", details);
        ServiceException serviceException = new ServiceException(error);
        String json = getJson(serviceException);
        assertTrue(validateJSON(json, "gsr/1.0/exception.json"));
    }
}
