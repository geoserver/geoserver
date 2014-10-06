/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.app;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.geoserver.python.Python;
import org.geoserver.rest.RestletException;
import org.geotools.util.logging.Logging;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFunction;
import org.python.core.PyIterator;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.python.core.PyTuple;
import org.python.util.PythonInterpreter;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Resource;

/**
 * Python resource that provides a WSGI environments for scripts to run in.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class PythonAppResource extends Resource {

    static Logger LOGGER = Logging.getLogger("org.geoserver.python");
    
    Python python;
    File appFile;
    
    static ThreadLocal<WSGIResponse> response = new ThreadLocal<WSGIResponse>();

    public PythonAppResource(Python python, File appFile, Request request, Response response) {
        super(null, request, response);
        this.python = python;
        this.appFile = appFile;
    }

    /**
     * Creates the environment object.
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
    * 
     * </pre>
     * @param request
     * @return
     */
    PyObject createEnviron(Request request) {
        
        PyDictionary environ = new PyDictionary();
        
        environ.put("REQUEST_METHOD", request.getMethod().toString());
        
        Reference ref = request.getResourceRef();
        environ.put("SCRIPT_NAME", ref.getLastSegment());
        
        Reference pref = ref.getParentRef();
        environ.put("PATH_INFO", pref.toString().substring(
            request.getRootRef().toString().length(), pref.toString().length()-1 ));
        
        environ.put("QUERY_STRING", request.getResourceRef().getQuery());

        //TODO: fill in rest of parameters
        return environ;
    }
    
    public static Object start_response(PyObject[] objs, String[] values) {
        PyString status = (PyString) objs[0];
        int space = status.toString().indexOf(' ');
        
        WSGIResponse r = response.get();
        if (space != -1) {
            r.statusCode = status.toString().substring(0, space);
            r.statusMessage = status.toString().substring(space+1);
        }
        else {
            r.statusCode = status.toString();
        }
        
        if (objs.length > 1) {
            PyList headers = (PyList) objs[1];
            for (Iterator i = headers.iterator(); i.hasNext();) {
                PyTuple tup = (PyTuple) i.next();
                r.headers.put(tup.get(0).toString(), tup.get(1).toString());
            }
        }
        return null;
    }
    
    PyFunction createStartResponse() {
        return new PyFunction(new PyStringMap(), new PyObject[]{}, Py.newJavaCode(getClass(), "start_response"));
    }
    
    @Override
    public void handleGet() {
        PythonInterpreter pi = python.interpreter();
        pi.execfile(appFile.getAbsolutePath());
        
        PyObject app = pi.get("app");
        if (app == null) {
            throw new RestletException("'app' function not found", Status.SERVER_ERROR_INTERNAL);
        }
        if (!(app instanceof PyFunction)) {
            throw new RestletException("'app' must be a function", Status.SERVER_ERROR_INTERNAL);
        }
        
        PyFunction appf = (PyFunction) app;
        PyFunction start_response = createStartResponse();
        
        WSGIResponse wr = new WSGIResponse(); 
        response.set(wr);
        
        PyObject ret = appf.__call__(new PyObject[]{createEnviron(getRequest()), start_response });
        if (ret != null) {
            
            String contentType = wr.headers.get("Content-type");
            if (contentType == null) {
                contentType = "text/plain";
            }
            
            MediaType mediaType = new MediaType(contentType);
            
            if (ret instanceof PyString) {
                getResponse().setEntity(ret.toString(), mediaType);
            }
            else if (ret instanceof PyList) {
                final PyList list = (PyList) ret;
                getResponse().setEntity(new OutputRepresentation(mediaType) {
                    
                    @Override
                    public void write(OutputStream outputStream) throws IOException {
                        for (Iterator i = list.iterator(); i.hasNext();) {
                            outputStream.write(i.next().toString().getBytes());
                            outputStream.write('\n');
                        }
                    }
                });
            }
            else if (ret instanceof PyIterator) {
                final PyIterator iter = (PyIterator) ret;
                getResponse().setEntity(new OutputRepresentation(mediaType) {
                    
                    @Override
                    public void write(OutputStream outputStream) throws IOException {
                        for (Iterator i = iter.iterator(); i.hasNext();) {
                            outputStream.write(i.next().toString().getBytes());
                            outputStream.write('\n');
                        }
                    }
                });
            }
            else {
                LOGGER.warning( "Unsure how to handle " + ret + ". Resorting to outputting string " +
                    "representation.");
                getResponse().setEntity(ret.toString(), mediaType);
            }
        }
        
        response.remove();
    }
    
    static class WSGIResponse {
        
        String statusCode;
        String statusMessage;
        TreeMap<String,String> headers = new TreeMap<String, String>();
    }
}
