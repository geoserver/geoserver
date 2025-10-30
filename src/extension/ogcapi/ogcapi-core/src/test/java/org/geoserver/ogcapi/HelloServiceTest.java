/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.ogcapi;

import static org.geoserver.ogcapi.MappingJackson2YAMLMessageConverter.APPLICATION_YAML_VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import jakarta.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.ows.TestDispatcherCallback;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.test.CodeExpectingHttpServletResponse;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

public class HelloServiceTest extends GeoServerSystemTestSupport {

    @Before
    public void cleanupCallbacks() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        dispatcher.callbacks.removeIf(c -> c instanceof TestDispatcherCallback);
    }

    @Before
    public void cleanupDefaultValue() throws Exception {
        HelloService controller = applicationContext.getBean(HelloService.class);
        controller.defaultValue = HelloService.DEFAULT_GREETING;
    }

    @Test
    public void testDefaultFormat() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();

        MockHttpServletRequest request = setupHelloRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals("{\"message\":\"hello\"}", response.getContentAsString());
    }

    @Test
    public void testQueryParameters() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();

        MockHttpServletRequest request = setupHelloRequest("message", "yo", "f", "json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals("{\"message\":\"yo\"}", response.getContentAsString());
    }

    @Test
    public void testYAMLFormatQueryParameter() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();

        MockHttpServletRequest request = setupHelloRequest("f", "yaml");
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(200, response.getStatus());
        assertEquals(APPLICATION_YAML_VALUE, response.getContentType());
        assertEquals("inline; filename=\"Message.yaml\"", response.getHeader(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("message: hello\n", response.getContentAsString());
    }

    @Test
    public void testYAMLFormatAcceptHeader() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();

        MockHttpServletRequest request = setupHelloRequest();
        request.addHeader(HttpHeaders.ACCEPT, APPLICATION_YAML_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(200, response.getStatus());
        assertEquals(APPLICATION_YAML_VALUE, response.getContentType());
        assertEquals("message: hello\n", response.getContentAsString());
    }

    @Test
    public void testPostRequest() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();

        String message = "{\"message\":\"Is there anyone here?\"}";
        MockHttpServletRequest request = setupEchoRequest(message, "f", "json");
        request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertEquals("Is there anyone here?", response.getContentAsString());
    }

    @Test
    public void testDeleteRequest() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();

        MockHttpServletRequest request = setupDeleteRequest();
        request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(204, response.getStatus());
    }

    @Test
    public void testPutRequest() throws Exception {

        APIDispatcher dispatcher = applicationContext.getBean(APIDispatcher.class);
        HelloService controller = applicationContext.getBean(HelloService.class);

        String newDefault = "ciao";
        MockHttpServletRequest request = setupPutRequest(newDefault);
        request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(200, response.getStatus());
        assertEquals(newDefault, controller.defaultValue);
    }

    @Test
    public void testDispatcherCallback() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        TestDispatcherCallback callback = new TestDispatcherCallback();

        MockHttpServletRequest request = setupHelloRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.callbacks.add(callback);

        dispatcher.handleRequest(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("{\"message\":\"hello\"}", response.getContentAsString());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackOperationName() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        AtomicReference<Request> requestReference = new AtomicReference<>();
        TestDispatcherCallback callback = new TestDispatcherCallback() {
            @Override
            public Operation operationDispatched(Request request, Operation operation) {
                requestReference.set(request);
                return operation;
            }
        };

        MockHttpServletRequest request = setupHelloRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.callbacks.add(callback);

        dispatcher.handleRequest(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("Hello", requestReference.get().getService());
        assertEquals("1.0.1", requestReference.get().getVersion());
        assertEquals("sayHello", requestReference.get().getRequest());
    }

    @Test
    public void testDispatcherCallbackFailInit() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();

        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        TestDispatcherCallback callbackFail = new TestDispatcherCallback() {
            @Override
            public Request init(Request request) {
                dispatcherStatus.set(Status.INIT);
                throw new RuntimeException("TestDispatcherCallbackFailInit");
            }
        };

        MockHttpServletRequest request = setupHelloRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);

        checkInternalError(response, "{\"type\":\"NoApplicableCode\",\"title\":\"TestDispatcherCallbackFailInit\"}");
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callbackFail.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackFailServiceDispatched() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        TestDispatcherCallback callbackFail = new TestDispatcherCallback() {
            @Override
            public Service serviceDispatched(Request request, Service service) {
                dispatcherStatus.set(Status.SERVICE_DISPATCHED);
                throw new RuntimeException("TestDispatcherCallbackFailServiceDispatched");
            }
        };

        MockHttpServletRequest request = setupHelloRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);

        checkInternalError(
                response, "{\"type\":\"NoApplicableCode\",\"title\":\"TestDispatcherCallbackFailServiceDispatched\"}");
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackFailOperationDispatched() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        TestDispatcherCallback callbackFail = new TestDispatcherCallback() {
            @Override
            public Operation operationDispatched(Request request, Operation operation) {
                dispatcherStatus.set(Status.OPERATION_DISPATCHED);
                throw new RuntimeException("TestDispatcherCallbackFailOperationDispatched");
            }
        };

        MockHttpServletRequest request = setupHelloRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);
        checkInternalError(
                response,
                "{\"type\":\"NoApplicableCode\",\"title\":\"TestDispatcherCallbackFailOperationDispatched\"}");
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackFailOperationExecuted() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        TestDispatcherCallback callbackFail = new TestDispatcherCallback() {
            @Override
            public Object operationExecuted(Request request, Operation operation, Object result) {
                dispatcherStatus.set(Status.OPERATION_EXECUTED);
                throw new RuntimeException("TestDispatcherCallbackFailOperationExecuted");
            }
        };

        MockHttpServletRequest request = setupHelloRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);

        checkInternalError(
                response, "{\"type\":\"NoApplicableCode\",\"title\":\"TestDispatcherCallbackFailOperationExecuted\"}");
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackFailResponseDispatched() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        TestDispatcherCallback callbackFail = new TestDispatcherCallback() {
            @Override
            public Response responseDispatched(Request request, Operation operation, Object result, Response response) {
                dispatcherStatus.set(Status.RESPONSE_DISPATCHED);
                throw new RuntimeException("TestDispatcherCallbackFailResponseDispatched");
            }
        };

        MockHttpServletRequest request = setupHelloRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);

        checkInternalError(
                response, "{\"type\":\"NoApplicableCode\",\"title\":\"TestDispatcherCallbackFailResponseDispatched\"}");
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackFailFinished() throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();
        final AtomicBoolean firedCallback = new AtomicBoolean(false);
        TestDispatcherCallback callback1 = new TestDispatcherCallback();
        TestDispatcherCallback callback2 = new TestDispatcherCallback() {
            @Override
            public void finished(Request request) {
                firedCallback.set(true);
                super.finished(request);
            }
        };
        TestDispatcherCallback callbackFail = new TestDispatcherCallback() {
            @Override
            public void finished(Request request) {
                dispatcherStatus.set(Status.FINISHED);
                // cleanups must continue even if an error was thrown
                throw new Error("TestDispatcherCallbackFailFinished");
            }
        };

        MockHttpServletRequest request = setupHelloRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals("{\"message\":\"hello\"}", response.getContentAsString());
        assertTrue(firedCallback.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testHttpErrorCodeException() throws Exception {
        assertHttpErrorCode("noContent", HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testWrappedHttpErrorCodeException() throws Exception {
        assertHttpErrorCode("wrappedException", HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    public void testBadRequestHttpErrorCodeException() throws Exception {
        assertHttpErrorCode("badRequest", HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testHttpErrorCodeExceptionWithContentType() throws Exception {
        CodeExpectingHttpServletResponse rsp = assertHttpErrorCode("errorWithPayload", HttpServletResponse.SC_OK);
        assertEquals("application/json", rsp.getContentType());
    }

    @Test
    public void testDocumentDefaultMime() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("ogc/hello/v1/document");
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        JSONObject json = (JSONObject) json(response);
        // links to self and alternate representations
        assertEquals("hello", json.get("message"));
        JSONArray links = json.getJSONArray("links");
        assertEquals(3, links.size());
        for (int i = 0; i < links.size(); i++) {
            JSONObject link = links.getJSONObject(i);
            if ("self".equals(link.getString("rel"))) {
                assertEquals("This document", link.getString("title"));
                assertEquals("application/json", link.getString("type"));
                assertEquals(
                        "http://localhost:8080/geoserver/ogc/hello/v1/document?f=application%2Fjson",
                        link.getString("href"));
            } else if ("alternate".equals(link.getString("rel")) && "application/yaml".equals(link.getString("type"))) {
                assertEquals("This document as application/yaml", link.getString("title"));
                assertEquals(
                        "http://localhost:8080/geoserver/ogc/hello/v1/document?f=application%2Fyaml",
                        link.getString("href"));
            } else if ("alternate".equals(link.getString("rel"))) {
                assertEquals("This document as text/html", link.getString("title"));
                assertEquals("text/html", link.getString("type"));
                assertEquals(
                        "http://localhost:8080/geoserver/ogc/hello/v1/document?f=text%2Fhtml", link.getString("href"));
            } else {
                fail("Unexpected link: " + link);
            }
        }
    }

    @Test
    public void testDocumentHTML() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("ogc/hello/v1/document?f=html");
        String contentAsString = response.getContentAsString();
        assertEquals(contentAsString, MediaType.TEXT_HTML_VALUE, response.getContentType());
        // Testing:
        // - the message is in the body
        // - service link generation
        // - resource link generation
        String expected =
                """
                <html>
                <head>
                    <script src="http://localhost:8080/geoserver/webresources/ogcapi/hello.js"></script>
                </head>
                <body>
                  <p>The message: hello</p>
                  <p><a class="wmsCapabilities" href="http://localhost:8080/geoserver/wms?request=GetCapabilities&amp;service=WMS">Capabilities URL</a></p>
                  <div>
                      <h2>value1</h2>
                      <h2>value2</h2>
                      <h2>value3</h2>
                  </div>
                </body>
                </html>""";
        // windows line endings are normalized to unix
        String normalizedResponse = response.getContentAsString().replaceAll("\r\n", "\n");
        assertEquals(expected, normalizedResponse);
    }

    /**
     * Verify {@link CloseableIterator} properties are properly closed by {@link AutoCloseableTracker} after encoding to
     * HTML
     */
    @Test
    public void testAutoCloseableTracker() throws Exception {
        AutoCloseableTracker.closed.set(0);
        MockHttpServletResponse response = getAsServletResponse("ogc/hello/v1/document?f=html");
        assertEquals(MediaType.TEXT_HTML_VALUE, response.getContentType());
        assertEquals(1, AutoCloseableTracker.closed.get());
    }

    /**
     * Verify {@link CloseableIterator} properties are properly closed by {@link CloseableIteratorSerializer} after
     * encoding to JSON
     */
    @Test
    public void testCloseableIteratorJsonSerializer() throws Exception {
        CloseableIteratorSerializer.closed.set(0);
        MockHttpServletResponse response = getAsServletResponse("ogc/hello/v1/document");
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(1, CloseableIteratorSerializer.closed.get());
    }

    @Test
    public void testHelloPlainText() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("ogc/hello/v1/hello?f=text/plain");
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertEquals("hello", response.getContentAsString());
    }

    @Test
    public void testeServiceDisabled() throws Exception {
        HelloService hs = applicationContext.getBean(HelloService.class);
        try {
            hs.getServiceInfo().setEnabled(false);
            MockHttpServletResponse response = getAsServletResponse("ogc/hello/v1");
            assertEquals(404, response.getStatus());
            assertEquals("Service Hello is disabled", response.getErrorMessage());
        } finally {
            hs.getServiceInfo().setEnabled(true);
        }
    }

    private CodeExpectingHttpServletResponse assertHttpErrorCode(String path, int expectedCode) throws Exception {
        APIDispatcher dispatcher = getAPIDispatcher();

        MockHttpServletRequest request = setupRequestBase();
        request.setMethod("GET");
        request.setServletPath("/geoserver/ogc");
        request.setPathInfo("hello/v1/" + path);
        request.setRequestURI("/geoserver/ogc/hello/v1/" + path);

        CodeExpectingHttpServletResponse response = new CodeExpectingHttpServletResponse(new MockHttpServletResponse());

        dispatcher.handleRequest(request, response);
        assertEquals(expectedCode, response.getStatusCode());

        assertEquals(expectedCode >= 400, response.isError());
        return response;
    }

    private APIDispatcher getAPIDispatcher() {
        return applicationContext.getBean(APIDispatcher.class);
    }

    private MockHttpServletRequest setupDeleteRequest() {
        MockHttpServletRequest request = setupRequestBase();
        request.setServletPath("/geoserver/ogc");
        request.setPathInfo("hello/v1/delete");
        request.setRequestURI("/geoserver/ogc/hello/v1/delete");
        request.setMethod("DELETE");

        return request;
    }

    private MockHttpServletRequest setupPutRequest(String message, String... params) {
        MockHttpServletRequest request = setupRequestBase(params);
        request.setServletPath("/geoserver/ogc");
        request.setPathInfo("hello/v1/default");
        request.setMethod("PUT");

        request.setRequestURI("/geoserver/ogc/hello/v1/default");
        request.setContent(message.getBytes(StandardCharsets.UTF_8));

        return request;
    }

    private MockHttpServletRequest setupEchoRequest(String message, String... params) {
        MockHttpServletRequest request = setupRequestBase(params);
        request.setServletPath("/geoserver/ogc");
        request.setPathInfo("hello/v1/hello");
        request.setMethod("POST");

        request.setRequestURI("/geoserver/ogc/hello/v1/echo");
        request.setContent(message.getBytes(StandardCharsets.UTF_8));

        return request;
    }

    private MockHttpServletRequest setupHelloRequest(String... params) {
        MockHttpServletRequest request = setupRequestBase(params);
        request.setServletPath("/geoserver/ogc");
        request.setPathInfo("hello/v1/hello");
        request.setMethod("GET");
        request.setRequestURI("/geoserver/ogc/hello/v1/hello");

        return request;
    }

    private MockHttpServletRequest setupRequestBase(String... params) {
        MockHttpServletRequest request = new MockHttpServletRequest() {
            String encoding;

            @Override
            public int getServerPort() {
                return 8080;
            }

            @Override
            public String getCharacterEncoding() {
                return encoding;
            }

            @Override
            public void setCharacterEncoding(String encoding) {
                this.encoding = encoding;
            }
        };

        request.setScheme("http");
        request.setServerName("localhost");

        request.setContextPath("/geoserver");
        request.setServletPath("/geoserver/ogc");

        if (params != null) {
            Map<String, String> map = toMap(params);
            map.forEach((k, v) -> request.addParameter(k, v));
            request.setQueryString(map.entrySet().stream().map(Object::toString).collect(Collectors.joining("&")));
        }

        RequestContextHolder.setRequestAttributes(new DispatcherServletWebRequest(request));
        return request;
    }

    public static Map<String, String> toMap(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) map.put(keyValues[i], keyValues[i + 1]);
        return map;
    }

    private void checkInternalError(MockHttpServletResponse response, String s) throws UnsupportedEncodingException {
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(500, response.getStatus());
        assertEquals(s, response.getContentAsString());
    }
}
