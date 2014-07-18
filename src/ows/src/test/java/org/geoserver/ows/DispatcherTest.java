/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.ServerException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.test.CodeExpectingHttpServletResponse;
import org.geotools.util.Version;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.web.servlet.ModelAndView;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockServletInputStream;


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

        MockServletInputStream input = new MockServletInputStream(body.getBytes());

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

        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/geoserver");

        request.setupAddParameter("service", "hello");
        request.setupAddParameter("request", "Hello");
        request.setupAddParameter("message", "Hello world!");

        request.setQueryString("service=hello&request=hello&message=Hello World!");

        Request req = new Request();
        req.setHttpRequest(request);

        dispatcher.parseKVP(req);

        Message message = (Message) dispatcher.parseRequestKVP(Message.class, req);
        assertEquals(new Message("Hello world!"), message);
    }

    public void testParseXML() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        String body = "<Hello service=\"hello\" message=\"Hello world!\"/>";
        File file = File.createTempFile("geoserver", "req");
        file.deleteOnExit();

        FileOutputStream output = new FileOutputStream(file);
        output.write(body.getBytes());
        output.flush();
        output.close();

        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

        input.mark(8192);

        Request req = new Request();
        req.setInput(input);

        Object object = dispatcher.parseRequestXML(null,input, req);
        assertEquals(new Message("Hello world!"), object);
    }

    public void testHelloOperationGet() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        MockHttpServletRequest request = new MockHttpServletRequest() {
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

        request.setupAddParameter("service", "hello");
        request.setupAddParameter("request", "Hello");
        request.setupAddParameter("version", "1.0.0");
        request.setupAddParameter("message", "Hello world!");

        request.setRequestURI(
            "http://localhost/geoserver/ows?service=hello&request=hello&message=HelloWorld");
        request.setQueryString("service=hello&request=hello&message=HelloWorld");
        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!", response.getOutputStreamContent());
    }

    public void testHelloOperationPost() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        final String body = "<Hello service=\"hello\" message=\"Hello world!\" version=\"1.0.0\" />";
        MockHttpServletRequest request = new MockHttpServletRequest() {
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

                public ServletInputStream getInputStream() throws IOException{
                    final ServletInputStream stream = super.getInputStream();
                    return new ServletInputStream(){
                        public int read() throws IOException{
                            return stream.read();
                        }

                        public int available(){
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
        request.setBodyContent(body);

        MockHttpServletResponse response = new MockHttpServletResponse();

        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!", response.getOutputStreamContent());
    }
    
    /**
     * Tests mixed get/post situations for cases in which there is no kvp parser
     * @throws Exception
     */
    public void testHelloOperationMixed() throws Exception {
        URL url = getClass().getResource("applicationContextOnlyXml.xml");

        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        final String body = "<Hello service=\"hello\" message=\"Hello world!\" version=\"1.0.0\" />";

        MockHttpServletRequest request = new MockHttpServletRequest() {
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
                
                public ServletInputStream getInputStream() throws IOException{
                    final ServletInputStream stream = super.getInputStream();
                    return new ServletInputStream(){
                        public int read() throws IOException{
                            return stream.read();
                        }

                        public int available(){
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
        request.setBodyContent(body);

        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setupAddParameter("strict", "true");

        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!", response.getOutputStreamContent());
    }
    
    public void testHttpErrorCodeException() throws Exception {
        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        MockHttpServletRequest request = new MockHttpServletRequest() {
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

        CodeExpectingHttpServletResponse response = new CodeExpectingHttpServletResponse(new MockHttpServletResponse());

        request.setupAddParameter("service", "hello");
        request.setupAddParameter("request", "httpErrorCodeException");
        request.setupAddParameter("version", "1.0.0");

        request.setRequestURI(
            "http://localhost/geoserver/ows?service=hello&request=hello&message=HelloWorld");
        request.setQueryString("service=hello&request=hello&message=HelloWorld");
        
        dispatcher.handleRequest(request, response);
        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatusCode());
    }
    
    /**
     * Assert that if the service bean implements the optional {@link DirectInvocationService}
     * operation, then the dispatcher executes the operation through its
     * {@link DirectInvocationService#invokeDirect} method instead of through {@link Method#invoke
     * reflection}.
     */
    public void testDirectInvocationService() throws Throwable {

        URL url = getClass().getResource("applicationContext.xml");

        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(
                url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        final AtomicBoolean invokeDirectCalled = new AtomicBoolean();
        DirectInvocationService serviceBean = new DirectInvocationService() {

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

        Service service = new Service("directCallService", serviceBean, new Version("1.0.0"),
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
        MockHttpServletRequest request = new MockHttpServletRequest() {
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
        request.setBodyContent("<h:Hello service='hello' message='Hello world!' xmlns:h='http://hello.org' />");
        request.setRequestURI("http://localhost/geoserver/hello");
        
        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!", response.getOutputStreamContent());

        request.setBodyContent("<h:Hello service='hello' message='Hello world!' xmlns:h='http://hello.org/v2' />");

        response = new MockHttpServletResponse();
        dispatcher.handleRequest(request, response);
        assertEquals("Hello world!:V2", response.getOutputStreamContent());
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
        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(
                url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");
        MockHttpServletRequest request = new MockHttpServletRequest()

        {
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
        request.setBodyContent("<h:Hello xmlns:h='http:/hello.org' />");
        request.setRequestURI("http://localhost/geoserver/hello");

        response = new MockHttpServletResponse();

        // Dispatch the request
        ModelAndView mov = dispatcher.handleRequestInternal(request, response);
        // Service exception, null is returned.
        assertNull(mov);
        // Check the response
        assertTrue(response.getOutputStreamContent().contains("Could not parse the XML"));
    }

    public void testDispatchKVPException() throws Exception {
        // This test ensures that the text of the exception indicates that a wrong KVP has been set
        URL url = getClass().getResource("applicationContext4.xml");

        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(
                url.toString());

        Dispatcher dispatcher = (Dispatcher) context.getBean("dispatcher");

        MockHttpServletRequest request = new MockHttpServletRequest()

        {
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
        request.setupAddParameter("request", "Hello");
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
        assertTrue(response.getOutputStreamContent().contains("Could not parse the KVP"));
    }
}
