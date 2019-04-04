/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;
import junit.framework.TestCase;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.ows.TestDispatcherCallback.Status;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.test.CodeExpectingHttpServletResponse;
import org.geotools.util.Version;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.Document;

public class DispatcherTest extends TestCase {
    public void testReadContextAndPath() throws Exception {
        Dispatcher dispatcher = new Dispatcher();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("get");

        Request req = new Request();
        req.httpRequest = request;

        dispatcher.init(req);
        assertNull(req.context);
        assertEquals("hello", req.path);

        request.setRequestURI("/geoserver/foo/hello");
        dispatcher.init(req);
        assertEquals("foo", req.context);
        assertEquals("hello", req.path);

        request.setRequestURI("/geoserver/foo/baz/hello/");
        dispatcher.init(req);
        assertEquals("foo/baz", req.context);
        assertEquals("hello", req.path);
    }

    public void testReadOpContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("get");

        Dispatcher dispatcher = new Dispatcher();

        Request req = new Request();
        req.httpRequest = request;
        dispatcher.init(req);

        Map map = dispatcher.readOpContext(req);

        assertEquals("hello", map.get("service"));

        request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/foobar/hello");
        request.setMethod("get");
        map = dispatcher.readOpContext(req);
        assertEquals("hello", map.get("service"));

        request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/foobar/hello/");
        request.setMethod("get");
        map = dispatcher.readOpContext(req);

        assertEquals("hello", map.get("service"));
    }

    public void testReadOpPost() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("post");

        String body = "<Hello service=\"hello\"/>";

        DelegatingServletInputStream input =
                new DelegatingServletInputStream(new ByteArrayInputStream(body.getBytes()));

        Dispatcher dispatcher = new Dispatcher();

        BufferedReader buffered = new BufferedReader(new InputStreamReader(input));
        buffered.mark(2048);

        Map map = dispatcher.readOpPost(buffered);

        assertNotNull(map);
        assertEquals("Hello", map.get("request"));
        assertEquals("hello", map.get("service"));
    }

    public void testParseKVP() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");

        request.addParameter("service", "hello");
        request.addParameter("request", "Hello");
        request.addParameter("message", "Hello world!");

        request.setQueryString("service=hello&request=hello&message=Hello World!");

        Request req = new Request();
        req.setHttpRequest(request);

        dispatcher.parseKVP(req);

        Message message = (Message) dispatcher.parseRequestKVP(Message.class, req);
        assertEquals(new Message("Hello world!"), message);
    }

    public void testParseXML() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        String body = "<Hello service=\"hello\" message=\"Hello world!\"/>";
        File file = File.createTempFile("geoserver", "req");
        try {

            FileOutputStream output = new FileOutputStream(file);
            output.write(body.getBytes());
            output.flush();
            output.close();

            BufferedReader input =
                    new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            input.mark(8192);

            Request req = new Request();
            req.setInput(input);

            Object object = dispatcher.parseRequestXML(null, input, req);
            assertEquals(new Message("Hello world!"), object);
        } finally {
            file.delete();
        }
    }

    public void testHelloOperationGet() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

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
        request.setMethod("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addParameter("service", "hello");
        request.addParameter("request", "Hello");
        request.addParameter("version", "1.0.0");
        request.addParameter("message", "Hello world!");

        request.setRequestURI(
                "http://localhost/geoserver/ows?service=hello&request=hello&message=HelloWorld");
        request.setQueryString("service=hello&request=hello&message=HelloWorld");

        dispatcher.callbacks.add(
                new AbstractDispatcherCallback() {
                    @Override
                    public Object operationExecuted(
                            Request request, Operation operation, Object result) {
                        Operation op = Dispatcher.REQUEST.get().getOperation();
                        assertNotNull(op);
                        assertTrue(op.getService().getService() instanceof HelloWorld);
                        assertTrue(op.getParameters()[0] instanceof Message);
                        return result;
                    }
                });

        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!", response.getContentAsString());
    }

    public void testHelloOperationPost() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        final String body =
                "<Hello service=\"hello\" message=\"Hello world!\" version=\"1.0.0\" />";
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

                    public ServletInputStream getInputStream() {
                        final ServletInputStream stream = super.getInputStream();
                        return new ServletInputStream() {
                            public int read() throws IOException {
                                return stream.read();
                            }

                            public int available() {
                                return body.length();
                            }
                        };
                    }
                };

        request.setScheme("http");
        request.setServerName("localhost");
        request.setContextPath("/geoserver");
        request.setMethod("POST");
        request.setRequestURI("http://localhost/geoserver/ows");
        request.setContentType("application/xml");
        request.setContent(body.getBytes("UTF-8"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!", response.getContentAsString());
    }

    /** Tests mixed get/post situations for cases in which there is no kvp parser */
    public void testHelloOperationMixed() throws Exception {
        URL url = getClass().getResource("applicationContextOnlyXml.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        final String body =
                "<Hello service=\"hello\" message=\"Hello world!\" version=\"1.0.0\" />";

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

                    public ServletInputStream getInputStream() {
                        final ServletInputStream stream = super.getInputStream();
                        return new ServletInputStream() {
                            public int read() throws IOException {
                                return stream.read();
                            }

                            public int available() {
                                return body.length();
                            }
                        };
                    }
                };

        request.setScheme("http");
        request.setServerName("localhost");
        request.setContextPath("/geoserver");
        request.setMethod("POST");
        request.setRequestURI("http://localhost/geoserver/ows");
        request.setContentType("application/xml");
        request.setContent(body.getBytes("UTF-8"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        request.addParameter("strict", "true");

        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!", response.getContentAsString());
    }

    public void testHttpErrorCodeException() throws Exception {
        assertHttpErrorCode("httpErrorCodeException", HttpServletResponse.SC_NO_CONTENT);
    }

    public void testWrappedHttpErrorCodeException() throws Exception {
        assertHttpErrorCode("wrappedHttpErrorCodeException", HttpServletResponse.SC_NO_CONTENT);
    }

    public void testBadRequestHttpErrorCodeException() throws Exception {
        assertHttpErrorCode("badRequestHttpErrorCodeException", HttpServletResponse.SC_BAD_REQUEST);
    }

    public void testHttpErrorCodeExceptionWithContentType() throws Exception {
        CodeExpectingHttpServletResponse rsp =
                assertHttpErrorCode(
                        "httpErrorCodeExceptionWithContentType", HttpServletResponse.SC_OK);
        assertEquals("application/json", rsp.getContentType());
    }

    private CodeExpectingHttpServletResponse assertHttpErrorCode(
            String requestType, int expectedCode) throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

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
        request.setMethod("GET");

        CodeExpectingHttpServletResponse response =
                new CodeExpectingHttpServletResponse(new MockHttpServletResponse());

        request.addParameter("service", "hello");
        request.addParameter("request", requestType);
        request.addParameter("version", "1.0.0");

        request.setRequestURI(
                "http://localhost/geoserver/ows?service=hello&request=hello&message=HelloWorld");
        request.setQueryString("service=hello&request=hello&message=HelloWorld");

        dispatcher.handleRequest(request, response);
        assertEquals(expectedCode, response.getStatusCode());

        assertEquals(expectedCode >= 400, response.isError());
        return response;
    }

    /**
     * Assert that if the service bean implements the optional {@link DirectInvocationService}
     * operation, then the dispatcher executes the operation through its {@link
     * DirectInvocationService#invokeDirect} method instead of through {@link Method#invoke
     * reflection}.
     */
    public void testDirectInvocationService() throws Throwable {

        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        final AtomicBoolean invokeDirectCalled = new AtomicBoolean();
        DirectInvocationService serviceBean =
                new DirectInvocationService() {

                    @Override
                    public Object invokeDirect(String operationName, Object[] parameters)
                            throws IllegalArgumentException, Exception {
                        invokeDirectCalled.set(true);
                        if ("concat".equals(operationName)) {
                            String param1 = (String) parameters[0];
                            String param2 = (String) parameters[1];
                            return concat(param1, param2);
                        }
                        throw new IllegalArgumentException("Unknown operation name");
                    }

                    public String concat(String param1, String param2) {
                        return param1 + param2;
                    }
                };

        Service service =
                new Service(
                        "directCallService",
                        serviceBean,
                        new Version("1.0.0"),
                        Collections.singletonList("concat"));
        Method method = serviceBean.getClass().getMethod("concat", String.class, String.class);
        Object[] parameters = {"p1", "p2"};
        Operation opDescriptor = new Operation("concat", service, method, parameters);

        Object result = dispatcher.execute(new Request(), opDescriptor);
        assertEquals("p1p2", result);
        assertTrue(invokeDirectCalled.get());
    }

    public void testDispatchWithNamespace() throws Exception {
        URL url = getClass().getResource("applicationContextNamespace.xml");
        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
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
        request.setMethod("POST");

        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setContentType("application/xml");
        request.setContent(
                "<h:Hello service='hello' message='Hello world!' xmlns:h='http://hello.org' />"
                        .getBytes("UTF-8"));
        request.setRequestURI("http://localhost/geoserver/hello");

        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!", response.getContentAsString());

        request.setContent(
                "<h:Hello service='hello' message='Hello world!' xmlns:h='http://hello.org/v2' />"
                        .getBytes("UTF-8"));

        response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!:V2", response.getContentAsString());
    }

    public MockHttpServletRequest setupRequest() {
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
        request.setMethod("GET");

        request.addParameter("service", "hello");
        request.addParameter("request", "Hello");
        request.addParameter("version", "1.0.0");
        request.addParameter("message", "Hello world!");

        request.setRequestURI(
                "http://localhost/geoserver/ows?service=hello&request=hello&message=HelloWorld");
        request.setQueryString("service=hello&request=hello&message=HelloWorld");

        return request;
    }

    public void testDispatcherCallback() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
        TestDispatcherCallback callback = new TestDispatcherCallback();

        MockHttpServletRequest request = setupRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback);

        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!", response.getContentAsString());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback.dispatcherStatus.get());
    }

    public void testDispatcherCallbackFailInit() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        final TestDispatcherCallback callback1 = new TestDispatcherCallback();
        final TestDispatcherCallback callback2 = new TestDispatcherCallback();
        ;
        TestDispatcherCallback callbackFail =
                new TestDispatcherCallback() {
                    @Override
                    public Request init(Request request) {
                        dispatcherStatus.set(Status.INIT);
                        throw new RuntimeException("TestDispatcherCallbackFailInit");
                    }
                };

        MockHttpServletRequest request = setupRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);

        assertTrue(response.getContentAsString().contains("ows:ExceptionReport"));
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    public void testDispatcherCallbackFailServiceDispatched() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
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

        MockHttpServletRequest request = setupRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);

        assertTrue(response.getContentAsString().contains("ows:ExceptionReport"));
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    public void testDispatcherCallbackFailOperationDispatched() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
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

        MockHttpServletRequest request = setupRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);

        assertTrue(response.getContentAsString().contains("ows:ExceptionReport"));
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    public void testDispatcherCallbackFailOperationExecuted() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
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

        MockHttpServletRequest request = setupRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);

        assertTrue(response.getContentAsString().contains("ows:ExceptionReport"));
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    public void testDispatcherCallbackFailResponseDispatched() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
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

        MockHttpServletRequest request = setupRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.callbacks.add(callback1);
        dispatcher.callbacks.add(callbackFail);
        dispatcher.callbacks.add(callback2);

        dispatcher.handleRequest(request, response);

        assertTrue(response.getContentAsString().contains("ows:ExceptionReport"));
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
        assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
    }

    public void testDispatcherCallbackFailFinished() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {
            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
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

            MockHttpServletRequest request = setupRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            dispatcher.callbacks.add(callback1);
            dispatcher.callbacks.add(callbackFail);
            dispatcher.callbacks.add(callback2);

            dispatcher.handleRequest(request, response);
            assertEquals("Hello world!", response.getContentAsString());
            assertTrue(firedCallback.get());
            assertEquals(TestDispatcherCallback.Status.FINISHED, callback1.dispatcherStatus.get());
            assertEquals(TestDispatcherCallback.Status.FINISHED, callback2.dispatcherStatus.get());
        }
    }

    public void testErrorSavedOnRequestOnGenericException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("get");

        Dispatcher dispatcher = new Dispatcher();

        Request req = new Request();
        req.httpRequest = request;
        dispatcher.init(req);

        MockHttpServletResponse response = new MockHttpServletResponse();
        req.setHttpResponse(response);

        RuntimeException genericError = new RuntimeException("foo");
        dispatcher.exception(genericError, null, req);

        assertEquals("Exception did not get saved", genericError, req.error);
    }

    public void testErrorSavedOnRequestOnNon304ErrorCodeException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("get");

        Dispatcher dispatcher = new Dispatcher();

        Request req = new Request();
        req.httpRequest = request;
        dispatcher.init(req);

        MockHttpServletResponse response = new MockHttpServletResponse();
        req.setHttpResponse(response);

        RuntimeException genericError = new HttpErrorCodeException(500, "Internal Server Error");
        dispatcher.exception(genericError, null, req);

        assertEquals("Exception did not get saved", genericError, req.error);
    }

    public void testNoErrorOn304ErrorCodeException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("get");

        Dispatcher dispatcher = new Dispatcher();

        Request req = new Request();
        req.httpRequest = request;
        dispatcher.init(req);

        MockHttpServletResponse response = new MockHttpServletResponse();
        req.setHttpResponse(response);

        RuntimeException error = new HttpErrorCodeException(304, "Not Modified");
        dispatcher.exception(error, null, req);

        assertNull("Exception erroneously saved", req.error);
    }

    public void testDispatchXMLException() throws Exception {
        // This test ensures that the text of the exception indicates that a wrong XML has been set
        URL url = getClass().getResource("applicationContextNamespace.xml");
        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
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
        request.setMethod("POST");

        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setContentType("application/xml");
        request.setContent("<h:Hello xmlns:h='http:/hello.org' />".getBytes("UTF-8"));
        request.setRequestURI("http://localhost/geoserver/hello");

        response = new MockHttpServletResponse();

        // Dispatch the request
        ModelAndView mov = dispatcher.handleRequestInternal(request, response);
        // Service exception, null is returned.
        assertNull(mov);
        // Check the response
        assertTrue(response.getContentAsString().contains("Could not parse the XML"));
    }

    public void testDispatchKVPException() throws Exception {
        // This test ensures that the text of the exception indicates that a wrong KVP has been set
        URL url = getClass().getResource("applicationContext4.xml");

        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

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
        request.setMethod("GET");

        // request.setupAddParameter("service", "hello");
        request.addParameter("request", "Hello");
        // request.setupAddParameter("message", "Hello world!");
        request.setRequestURI("http://localhost/geoserver/hello");

        request.setQueryString("message=Hello World!");

        MockHttpServletResponse response = new MockHttpServletResponse();

        response = new MockHttpServletResponse();

        // Dispatch the request
        ModelAndView mov = dispatcher.handleRequestInternal(request, response);
        // Service exception, null is returned.
        assertNull(mov);
        // Check the response
        assertTrue(response.getContentAsString().contains("Could not parse the KVP"));
    }

    public void testMultiPartFormUpload() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("post");

        String xml = "<Hello service='hello' message='Hello world!' version='1.0.0' />";

        MimeMultipart body = new MimeMultipart();
        request.setContentType(body.getContentType());

        InternetHeaders headers = new InternetHeaders();
        headers.setHeader(
                "Content-Disposition", "form-data; name=\"upload\"; filename=\"request.xml\"");
        headers.setHeader("Content-Type", "application/xml");
        body.addBodyPart(new MimeBodyPart(headers, xml.getBytes()));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        body.writeTo(bout);

        request.setContent(bout.toByteArray());

        MockHttpServletResponse response = new MockHttpServletResponse();

        URL url = getClass().getResource("applicationContext.xml");
        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());
        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
        dispatcher.handleRequestInternal(request, response);

        assertEquals("Hello world!", response.getContentAsString());
    }

    public void testMultiPartFormUploadWithBodyField() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");
        request.setRequestURI("/geoserver/hello");
        request.setMethod("post");

        String xml = "<Hello service='hello' message='Hello world!' version='1.0.0' />";

        MimeMultipart body = new MimeMultipart();
        request.setContentType(body.getContentType());

        InternetHeaders headers = new InternetHeaders();
        headers.setHeader("Content-Disposition", "form-data; name=\"body\";");
        headers.setHeader("Content-Type", "application/xml");
        body.addBodyPart(new MimeBodyPart(headers, xml.getBytes()));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        body.writeTo(bout);

        request.setContent(bout.toByteArray());

        MockHttpServletResponse response = new MockHttpServletResponse();

        URL url = getClass().getResource("applicationContext.xml");
        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString());
        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
        dispatcher.handleRequestInternal(request, response);

        assertEquals("Hello world!", response.getContentAsString());
    }

    public void testErrorThrowingResponse() throws Exception {
        URL url = getClass().getResource("applicationContext-errorResponse.xml");

        try (FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(url.toString())) {
            Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
            MockHttpServletRequest request = setupRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();
            dispatcher.handleRequest(request, response);
            // the output is not there
            final String outputContent = response.getContentAsString();
            assertThat(outputContent, not(containsString("Hello world!")));
            // only the exception
            Document dom = XMLUnit.buildTestDocument(outputContent);
            assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        }
    }
}
