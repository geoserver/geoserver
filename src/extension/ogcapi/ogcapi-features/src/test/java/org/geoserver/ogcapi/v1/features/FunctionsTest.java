/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.github.erosb.jsonsKema.FormatValidationPolicy;
import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonValue;
import com.github.erosb.jsonsKema.Schema;
import com.github.erosb.jsonsKema.SchemaLoader;
import com.github.erosb.jsonsKema.ValidationFailure;
import com.github.erosb.jsonsKema.Validator;
import com.github.erosb.jsonsKema.ValidatorConfig;
import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.SystemTestData;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class FunctionsTest extends FeaturesTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed
    }

    @Test
    public void testSchemaValid() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("ogc/features/v1/functions");

        // check the response abides to the functions schema
        Schema schema = SchemaLoader.forURL("classpath:/org/geoserver/ogcapi/v1/features/functions-schema.yml")
                .load();
        JsonValue functionsJSON = new JsonParser(response.getContentAsString()).parse();
        Validator validator = Validator.create(schema, new ValidatorConfig(FormatValidationPolicy.ALWAYS));
        ValidationFailure failure = validator.validate(functionsJSON);
        assertNull(failure);
    }

    @Test
    public void testFunctions() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1/functions", 200);

        // test one random function
        DocumentContext function = readSingleContext(json, "functions[?(@.name=='strSubstring')]");
        assertEquals("string", function.read("returns[0]"));
        assertEquals("string", function.read("arguments[0].title"));
        assertEquals("string", readSingle(function, "arguments[0].type"));
        assertEquals("beginIndex", function.read("arguments[1].title"));
        assertEquals("integer", readSingle(function, "arguments[1].type"));
        assertEquals("endIndex", function.read("arguments[2].title"));
        assertEquals("integer", readSingle(function, "arguments[2].type"));
    }

    @Test
    public void testFunctionsHTML() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("ogc/features/v1/functions?f=html");
        assertEquals("text/html", response.getContentType());
        // the table is hard to look up, so let's check the representation for a given function
        String normalizedResponse = response.getContentAsString().replaceAll("\r\n", "\n");
        assertThat(
                normalizedResponse,
                Matchers.containsString("<h3>Area</h3>\n"
                        + "         <ul>\n"
                        + "         <li>Returns: number </li>\n"
                        + "         <li>Arguments:\n"
                        + "         <table class=\"function-table\">\n"
                        + "         <tr><th>Name</th><th>Title</th><th>Type</th></tr>\n"
                        + "            <tr><td>geometry</td><td>geometry</td><td>geometry </td></tr>\n"
                        + "         </table>\n"
                        + "         </li>\n"
                        + "         </ul>"));
    }
}
