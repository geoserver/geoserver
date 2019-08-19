/*
 *  (c) 2019 Open Source Geospatial Foundation - all rights reserved
 *  This code is licensed under the GPL 2.0 license, available at the root
 *  application directory.
 */

package org.geoserver.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.ows.TestDispatcherCallback;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.test.CodeExpectingHttpServletResponse;
import org.junit.Test;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.handler.DispatcherServletWebRequest;

public class APIDispatcherTest {

    @Test
    public void testDefaultFormat() throws Exception {
        APIDispatcher dispatcher = getDispatcher();

        MockHttpServletRequest request = setupHelloRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals("{\"message\":\"hello\"}", response.getContentAsString());
    }

    @Test
    public void testQueryParameters() throws Exception {
        APIDispatcher dispatcher = getDispatcher();

        MockHttpServletRequest request = setupHelloRequest("message", "yo", "f", "json");
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals("{\"message\":\"yo\"}", response.getContentAsString());
    }

    @Test
    public void testXMLFormatQueryParameter() throws Exception {
        APIDispatcher dispatcher = getDispatcher();

        MockHttpServletRequest request = setupHelloRequest("f", "xml");
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());
        assertEquals("<Message><message>hello</message></Message>", response.getContentAsString());
    }

    @Test
    public void testXMLFormatAcceptHeader() throws Exception {
        APIDispatcher dispatcher = getDispatcher();

        MockHttpServletRequest request = setupHelloRequest();
        request.addHeader(HttpHeaders.ACCEPT, "application/xml");
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_XML_VALUE, response.getContentType());
        assertEquals("<Message><message>hello</message></Message>", response.getContentAsString());
    }

    @Test
    public void testPostRequest() throws Exception {
        APIDispatcher dispatcher = getDispatcher();

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
        APIDispatcher dispatcher = getDispatcher();

        String message = "{\"message\":\"Is there anyone here?\"}";
        MockHttpServletRequest request = setupDeleteRequest();
        request.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);

        assertEquals(204, response.getStatus());
    }

    @Test
    public void testPutRequest() throws Exception {
        FileSystemXmlApplicationContext context = getApplicationContext();
        APIDispatcher dispatcher = (APIDispatcher) context.getBean(APIDispatcher.class);
        HelloController controller = (HelloController) context.getBean(HelloController.class);

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
        APIDispatcher dispatcher = getDispatcher();
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
        APIDispatcher dispatcher = getDispatcher();
        AtomicReference<Request> requestReference = new AtomicReference<>();
        TestDispatcherCallback callback =
                new TestDispatcherCallback() {
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
        assertEquals("1.0", requestReference.get().getVersion());
        assertEquals("sayHello", requestReference.get().getRequest());
    }

    @Test
    public void testDispatcherCallbackFailInit() throws Exception {
        APIDispatcher dispatcher = getDispatcher();

        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        TestDispatcherCallback callbackFail =
                new TestDispatcherCallback() {
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

        checkInternalError(
                response,
                "{\"code\":\"NoApplicableCode\",\"description\":\"TestDispatcherCallbackFailInit\"}");
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callbackFail.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackFailServiceDispatched() throws Exception {
        APIDispatcher dispatcher = getDispatcher();
        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        TestDispatcherCallback callbackFail =
                new TestDispatcherCallback() {
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
                response,
                "{\"code\":\"NoApplicableCode\",\"description\":\"TestDispatcherCallbackFailServiceDispatched\"}");
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackFailOperationDispatched() throws Exception {
        APIDispatcher dispatcher = getDispatcher();
        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        TestDispatcherCallback callbackFail =
                new TestDispatcherCallback() {
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
                "{\"code\":\"NoApplicableCode\",\"description\":\"TestDispatcherCallbackFailOperationDispatched\"}");
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackFailOperationExecuted() throws Exception {
        APIDispatcher dispatcher = getDispatcher();
        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        TestDispatcherCallback callbackFail =
                new TestDispatcherCallback() {
                    @Override
                    public Object operationExecuted(
                            Request request, Operation operation, Object result) {
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
                response,
                "{\"code\":\"NoApplicableCode\",\"description\":\"TestDispatcherCallbackFailOperationExecuted\"}");
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackFailResponseDispatched() throws Exception {
        APIDispatcher dispatcher = getDispatcher();
        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        TestDispatcherCallback callbackFail =
                new TestDispatcherCallback() {
                    @Override
                    public Response responseDispatched(
                            Request request,
                            Operation operation,
                            Object result,
                            Response response) {
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
                response,
                "{\"code\":\"NoApplicableCode\",\"description\":\"TestDispatcherCallbackFailResponseDispatched\"}");
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    @Test
    public void testDispatcherCallbackFailFinished() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        APIDispatcher dispatcher = getDispatcher();
        final AtomicBoolean firedCallback = new AtomicBoolean(false);
        TestDispatcherCallback callback1 = new TestDispatcherCallback();
        TestDispatcherCallback callback2 =
                new TestDispatcherCallback() {
                    @Override
                    public void finished(Request request) {
                        firedCallback.set(true);
                        super.finished(request);
                    }
                };
        TestDispatcherCallback callbackFail =
                new TestDispatcherCallback() {
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
        CodeExpectingHttpServletResponse rsp =
                assertHttpErrorCode("errorWithPayload", HttpServletResponse.SC_OK);
        assertEquals("application/json", rsp.getContentType());
    }

    private CodeExpectingHttpServletResponse assertHttpErrorCode(String path, int expectedCode)
            throws Exception {
        APIDispatcher dispatcher = getDispatcher();

        MockHttpServletRequest request = setupRequestBase();
        request.setMethod("GET");
        request.setPathInfo("/geoserver/ogc/" + path);
        request.setRequestURI("/geoserver/ogc/" + path);

        CodeExpectingHttpServletResponse response =
                new CodeExpectingHttpServletResponse(new MockHttpServletResponse());

        dispatcher.handleRequest(request, response);
        assertEquals(expectedCode, response.getStatusCode());

        assertEquals(expectedCode >= 400, response.isError());
        return response;
    }

    private APIDispatcher getDispatcher() {
        FileSystemXmlApplicationContext context = getApplicationContext();
        return (APIDispatcher) context.getBean(APIDispatcher.class);
    }

    private FileSystemXmlApplicationContext getApplicationContext() {
        URL url = getClass().getResource("applicationContext.xml");
        return new FileSystemXmlApplicationContext(url.toString());
    }

    private MockHttpServletRequest setupDeleteRequest() {
        MockHttpServletRequest request = setupRequestBase();
        request.setPathInfo("/geoserver/ogc/delete");
        request.setRequestURI("/geoserver/ogc/delete");
        request.setMethod("DELETE");

        return request;
    }

    private MockHttpServletRequest setupPutRequest(String message, String... params) {
        MockHttpServletRequest request = setupRequestBase(params);
        request.setPathInfo("/geoserver/ogc/default");
        request.setMethod("PUT");

        request.setRequestURI("/geoserver/ogc/default");
        request.setContent(message.getBytes(Charset.forName("UTF-8")));

        return request;
    }

    private MockHttpServletRequest setupEchoRequest(String message, String... params) {
        MockHttpServletRequest request = setupRequestBase(params);
        request.setPathInfo("/geoserver/ogc/hello");
        request.setMethod("POST");

        request.setRequestURI("/geoserver/ogc/echo");
        request.setContent(message.getBytes(Charset.forName("UTF-8")));

        return request;
    }

    private MockHttpServletRequest setupHelloRequest(String... params) {
        MockHttpServletRequest request = setupRequestBase(params);
        request.setPathInfo("/geoserver/ogc/hello");
        request.setMethod("GET");
        request.setRequestURI("/geoserver/ogc/hello");

        return request;
    }

    private MockHttpServletRequest setupRequestBase(String... params) {
        MockHttpServletRequest request =
                new MockHttpServletRequest() {
                    String encoding;

                    public int getServerPort() {
                        return 8080;
                    }

                    public String getCharacterEncoding() {
                        return encoding;
                    }

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
            request.setQueryString(
                    map.entrySet().stream().map(Object::toString).collect(Collectors.joining("&")));
        }

        RequestContextHolder.setRequestAttributes(new DispatcherServletWebRequest(request));
        return request;
    }

    public static Map<String, String> toMap(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) map.put(keyValues[i], keyValues[i + 1]);
        return map;
    }

    private void checkInternalError(MockHttpServletResponse response, String s)
            throws UnsupportedEncodingException {
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals(500, response.getStatus());
        assertEquals(s, response.getContentAsString());
    }
}
