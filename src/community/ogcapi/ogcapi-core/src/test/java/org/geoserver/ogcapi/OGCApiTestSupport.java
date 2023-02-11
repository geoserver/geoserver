/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.internal.JsonContext;
import com.jayway.jsonpath.internal.JsonFormatter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import net.minidev.json.JSONAware;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.filters.SpringDelegatingFilter;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matchers;
import org.springframework.mock.web.MockHttpServletResponse;

public class OGCApiTestSupport extends GeoServerSystemTestSupport {

    @Override
    protected List<Filter> getFilters() {
        // needed for proxy base tests
        try {
            SpringDelegatingFilter filter = new SpringDelegatingFilter();
            filter.init(null);
            return Collections.singletonList(filter);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse(path, expectedHttpCode);
        return getAsJSONPath(response);
    }

    protected DocumentContext postAsJSONPath(String path, String body, int expectedHttpCode)
            throws Exception {
        MockHttpServletResponse response = postAsServletResponse(path, body, "application/json");
        assertEquals(expectedHttpCode, response.getStatus());
        return getAsJSONPath(response);
    }

    protected DocumentContext getAsJSONPath(MockHttpServletResponse response)
            throws UnsupportedEncodingException {
        assertThat(response.getContentType(), containsString("json"));
        JsonContext json = (JsonContext) JsonPath.parse(response.getContentAsString());
        if (!isQuietTests()) {
            print(json(response));
        }
        return json;
    }

    protected MockHttpServletResponse getAsMockHttpServletResponse(
            String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);

        assertEquals(expectedHttpCode, response.getStatus());
        return response;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        registerNamespaces(namespaces);

        CiteTestData.registerNamespaces(namespaces);

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    /**
     * Allows subclasses to register namespaces. By default, does not add any, subclasses can
     * manipulate the namespaces map as they see fit.
     */
    protected void registerNamespaces(Map<String, String> namespaces) {}

    protected byte[] getAsByteArray(String url) throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse(url, 200);
        return response.getContentAsByteArray();
    }

    protected JsonContext convertYamlToJsonPath(String yaml) throws Exception {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        JsonContext json = (JsonContext) JsonPath.parse(jsonWriter.writeValueAsString(obj));
        return json;
    }

    /**
     * Returns a single element out of an array, checking that there is just one. Works around
     * Workaround for https://github.com/json-path/JsonPath/issues/272
     */
    @SuppressWarnings("unchecked")
    protected <T> T readSingle(DocumentContext json, String path) {
        List<Object> items = json.read(path);
        assertEquals(
                "Found "
                        + items.size()
                        + " items for this path, but was expecting one: "
                        + path
                        + "\n"
                        + items,
                1,
                items.size());
        return (T) items.get(0);
    }

    /**
     * Evaluates the path, which is supposed to return a JSON like object. Turns it into a string,
     * and then turns it again into a {@link DocumentContext}.
     *
     * <p>Useful to create a sub-section of a JSON document for further analysis, allows to cut on
     * JSONPath complexity.
     *
     * @param ctx The parent context
     * @param path The JSON Path to be evaluated
     * @return The context derived from the JSON Path evaluation
     */
    protected DocumentContext readContext(DocumentContext ctx, String path) {
        JSONAware result = ctx.read(path, JSONAware.class);
        return JsonPath.parse(result.toJSONString());
    }

    /**
     * Similar to {@link #readSingle(DocumentContext, String)} but returns a {@link
     * DocumentContext}. Workaround for https://github.com/json-path/JsonPath/issues/272.
     *
     * @param ctx The parent context
     * @param path The JSON Path to be evaluated, assumes it's returning an array of one item
     * @return The context derived from the JSON Path evaluation
     */
    protected DocumentContext readSingleContext(DocumentContext ctx, String path) {
        List list = ctx.read(path, List.class);
        if (list.size() != 1)
            throw new RuntimeException(
                    "Was expecting to get an array of one, but got "
                            + list.size()
                            + " elements instead");
        // remove the array markers around the json we want (ugly!, could not find another way)
        String array = ctx.read(path, JSONAware.class).toJSONString();
        int opening = array.indexOf("[");
        int closing = array.lastIndexOf("]");
        String content = array.substring(opening + 1, closing);
        return JsonPath.parse(content);
    }

    /**
     * Prints the contents of a document on the standard output. Won't show up during Maven
     * execution due to the quiet test setting, but will be executed when running tests directly
     * (e.g., from an IDE)
     */
    @SuppressWarnings("PMD.SystemPrintln")
    protected void print(DocumentContext json) {
        if (isQuietTests()) {
            return;
        }
        System.out.println(JsonFormatter.prettyPrint(json.jsonString()));
    }

    /** Checks the specified jsonpath exists in the document */
    protected boolean exists(DocumentContext json, String path) {
        try {
            List items = json.read(path);
            return !items.isEmpty();
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /**
     * Verifies the given JSONPath evaluates to the expected list
     *
     * @param json The document
     * @param path The path
     * @param expected The expected list
     * @param <T>
     */
    @SuppressWarnings("unchecked")
    protected <T> void assertJSONList(DocumentContext json, String path, T... expected) {
        List<T> selfRels = json.read(path);
        assertThat(selfRels, Matchers.containsInAnyOrder(expected));
    }

    /**
     * Verifies that the given header is present in the response, and that it contains the expected
     * value
     *
     * @param response the response
     * @param headerName the header name
     * @param expectedValue the expected value
     * @return true if the header is present and contains the expected value
     */
    protected boolean headerHasValue(
            MockHttpServletResponse response, String headerName, String expectedValue) {
        String headerValue = response.getHeader(headerName);
        return headerValue != null && headerValue.contains(expectedValue);
    }
}
