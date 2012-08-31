package org.opengeo.gsr.core.exception;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.opengeo.gsr.validation.JSONSchemaUtils;

public class ExceptionJSONSchemaTest extends JSONSchemaUtils {

    ObjectMapper mapper;

    public ExceptionJSONSchemaTest() {
        mapper = new ObjectMapper();
    }

    @Test
    public void testServiceErrorJSONSchema() throws Exception {
        List<String> details = new ArrayList<String>();
        details.add("Bad request details");
        ServiceError error = new ServiceError(400, "Bad Request", details);
        String json = mapper.writeValueAsString(error);
        assertTrue(validateJSON(json, "gsr/1.0/error.json"));
    }

    @Test
    public void testServiceExceptionJSONSchema() throws Exception {
        List<String> details = new ArrayList<String>();
        details.add("Bad request details");
        ServiceError error = new ServiceError(400, "Bad Request", details);
        ServiceException serviceException = new ServiceException(error);
        String json = mapper.writeValueAsString(serviceException);
        assertTrue(validateJSON(json, "gsr/1.0/exception.json"));
    }
}
