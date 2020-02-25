/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.script.app.AppHook;
import org.geotools.util.logging.Logging;
import org.python.core.*;

/**
 * Python app hook.
 *
 * <p>This app hook adapts the incoming request into a WSGI request requiring the app script to
 * implement a WSGI interface via a function named "app". See <a
 * href="http://en.wikipedia.org/wiki/Web_Server_Gateway_Interface">web service gateway
 * interface</a> for more details about WSGI.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class PyAppHook extends AppHook {

    static Logger LOGGER = Logging.getLogger(PyAppHook.class);
    static ThreadLocal<WSGIResponse> RESPONSE = new ThreadLocal<WSGIResponse>();

    public PyAppHook(PythonPlugin plugin) {
        super(plugin);
    }

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response, ScriptEngine engine)
            throws ScriptException, IOException {

        Object obj = engine.get("app");
        if (obj == null) {
            throw new RuntimeException("No 'app' function defined");
        }

        if (!(obj instanceof PyObject)) {
            throw new RuntimeException("'app not callable, found a " + obj.toString());
        }

        PyObject app = (PyObject) obj;

        WSGIResponse wr = new WSGIResponse();
        RESPONSE.set(wr);
        try {
            Object ret =
                    app.__call__(new PyObject[] {createEnviron(request), createStartResponse()});
            if (ret != null) {

                String contentType = wr.headers.get("content-type");
                if (contentType == null) {
                    contentType = "text/plain";
                }
                response.setContentType(contentType);
                if (ret instanceof PyString) {
                    response.getWriter().write(ret.toString());
                } else if (ret instanceof PyList) {
                    final PyList list = (PyList) ret;
                    for (Iterator i = list.iterator(); i.hasNext(); ) {
                        response.getWriter().write(i.next().toString());
                        if (i.hasNext()) {
                            response.getWriter().write('\n');
                        }
                    }
                } else if (ret instanceof PyIterator) {
                    final PyIterator iter = (PyIterator) ret;
                    for (Iterator i = iter.iterator(); i.hasNext(); ) {
                        response.getWriter().write(i.next().toString());
                        response.getWriter().write('\n');
                    }
                } else if (ret instanceof PyObjectDerived) {
                    final PyObjectDerived iter = (PyObjectDerived) ret;
                    PyObject next = null;
                    while ((next = iter.__iternext__()) != null) {
                        response.getWriter().write(next.toString());
                        response.getWriter().write('\n');
                    }
                } else {
                    LOGGER.warning(
                            "Unsure how to handle "
                                    + ret
                                    + ". Resorting to outputing string "
                                    + "representation.");
                    response.setContentType(contentType);
                    response.getWriter().write(ret.toString());
                }
            }
        } finally {
            RESPONSE.remove();
        }
    }

    /**
     * Creates the environ object which is a dictionary with the following entries:
     *
     * <pre>
     * REQUEST_METHOD
     *       The HTTP request method, such as "GET" or "POST". This cannot ever be
     *       an empty string, and so is always required.
     * SCRIPT_NAME
     *       The initial portion of the request URL's "path" that corresponds to the
     *       application object, so that the application knows its virtual "location"
     *       . This may be an empty string, if the application corresponds to the
     *       "root" of the server.
     * PATH_INFO
     *       The remainder of the request URL's "path", designating the virtual
     *       "location" of the request's target within the application. This may be
     *        an empty string, if the request URL targets the application root and
     *       does not have a trailing slash.
     * QUERY_STRING
     *       The portion of the request URL that follows the "?", if any. May be
     *       empty or absent.
     * CONTENT_TYPE
     *       The contents of any Content-Type fields in the HTTP request. May be
     *       empty or absent.
     * CONTENT_LENGTH
     *       The contents of any Content-Length fields in the HTTP request. May be
     *       empty or absent.
     * SERVER_NAME, SERVER_PORT
     *       When combined with SCRIPT_NAME and PATH_INFO, these variables can be
     *       used to complete the URL. Note, however, that HTTP_HOST, if present,
     *       should be used in preference to SERVER_NAME for reconstructing the
     *       request URL. See the URL Reconstruction section below for more detail.
     *       SERVER_NAME and SERVER_PORT can never be empty strings, and so are
     *       always required.
     * SERVER_PROTOCOL
     *       The version of the protocol the client used to send the request.
     *       Typically this will be something like "HTTP/1.0" or "HTTP/1.1" and may
     *       be used by the application to determine how to treat any HTTP request
     *       headers. (This variable should probably be called REQUEST_PROTOCOL,
     *       since it denotes the protocol used in the request, and is not
     *       necessarily the protocol that will be used in the server's response.
     *       However, for compatibility with CGI we have to keep the existing name.)
     * HTTP_ Variables
     *       Variables corresponding to the client-supplied HTTP request headers
     *       (i.e., variables whose names begin with "HTTP_"). The presence or
     *       absence of these variables should correspond with the presence or
     *       absence of the appropriate HTTP header in the request.
     * wsgi.version
     *   The tuple (1, 0), representing WSGI version 1.0.
     *
     * wsgi.url_scheme
     *   A string representing the "scheme" portion of the URL at which the application
     *   is being invoked. Normally, this will have the value "http" or "https", as
     *   appropriate.
     *
     * wsgi.input
     *   An input stream (file-like object) from which the HTTP request body can be
     *   read. (The server or gateway may perform reads on-demand as requested by the
     *   application, or it may pre- read the client's request body and buffer it
     *   in-memory or on disk, or use any other technique for providing such an input
     *   stream, according to its preference.)
     *
     * wsgi.errors
     *   An output stream (file-like object) to which error output can be written, for
     *   the purpose of recording program or other errors in a standardized and
     *   possibly centralized location. This should be a "text mode" stream; i.e.,
     *   applications should use "\n" as a line ending, and assume that it will be
     *   converted to the correct line ending by the server/gateway.
     *
     *   For many servers, wsgi.errors will be the server's main error log.
     *   Alternatively, this may be sys.stderr, or a log file of some sort. The
     *   server's documentation should include an explanation of how to configure this
     *   or where to find the recorded output. A server or gateway may supply different
     *   error streams to different applications, if this is desired.
     *
     * wsgi.multithread
     *   This value should evaluate true if the application object may be
     *   simultaneously invoked by another thread in the same process, and should
     *   evaluate false otherwise.
     *
     * wsgi.multiprocess
     *   This value should evaluate true if an equivalent application object may be
     *   simultaneously invoked by another process, and should evaluate false
     *   otherwise.
     *
     * wsgi.run_once
     *   This value should evaluate true if the server or gateway expects (but does not
     *   guarantee!) that the application will only be invoked this one time during the
     *   life of its containing process. Normally, this will only be true for a gateway
     *   based on CGI (or something similar).
     * </pre>
     */
    PyObject createEnviron(HttpServletRequest request) throws IOException {

        PyDictionary environ = new PyDictionary();

        environ.put("REQUEST_METHOD", request.getMethod().toString());

        environ.put(
                "SCRIPT_NAME",
                request.getPathInfo().substring(request.getPathInfo().lastIndexOf("/")));

        // force to pystring so that frameworks don't try to encode as idna
        environ.put("SERVER_NAME", new PyString(request.getServerName()));
        environ.put("SERVER_PORT", String.valueOf(request.getServerPort()));

        environ.put("PATH_INFO", request.getPathInfo());
        // environ.put("PATH_INFO", );

        environ.put("QUERY_STRING", request.getQueryString());

        environ.put("wsgi.version", new PyTuple(new PyInteger(0), new PyInteger(1)));
        environ.put("wsgi.url_scheme", request.getScheme());
        environ.put("wsgi.input", new PyFile(request.getInputStream()));
        environ.put("wsgi.errors", new PyFile(System.err));
        environ.put("wsgi.multithread", true);
        environ.put("wsgi.multitprocess", false);
        environ.put("wsgi.run_once", false);
        return environ;
    }

    /** Creates the start_response object. */
    PyFunction createStartResponse() {
        return new PyFunction(
                new PyStringMap(), new PyObject[] {}, Py.newJavaCode(getClass(), "start_response"));
    }

    public static Object start_response(PyObject[] objs, String[] values) {
        PyString status = (PyString) objs[0];
        int space = status.toString().indexOf(' ');

        WSGIResponse r = RESPONSE.get();
        if (space != -1) {
            r.code = status.toString().substring(0, space);
            r.message = status.toString().substring(space + 1);
        } else {
            r.code = status.toString();
        }

        if (objs.length > 1) {
            PyList headers = (PyList) objs[1];
            for (Iterator i = headers.iterator(); i.hasNext(); ) {
                PyTuple tup = (PyTuple) i.next();
                r.headers.put(tup.get(0).toString(), tup.get(1).toString());
            }
        }
        return null;
    }

    static class WSGIResponse {
        String code;
        String message;
        Map<String, String> headers = new CaseInsensitiveMap(new TreeMap<String, String>());
    }
}
