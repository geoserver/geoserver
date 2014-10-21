/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.script.ScriptException;

import org.geoserver.script.js.engine.CommonJSEngine;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.BoundFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;

public class JsgiResponse {

    private int status = 200;
    private ScriptableObject headers;
    private Scriptable body;
    private Function forEach;
    
    static Logger LOGGER = Logging.getLogger("org.geoserver.script.js");
        
    public JsgiResponse(Scriptable obj, Scriptable scope) {

        // extract status
        Object statusObj = obj.get("status", obj);
        if (statusObj instanceof Integer) {
            status = (Integer) statusObj;
        }
        
        // extract headers
        Object headersObj = obj.get("headers", obj);
        if (headersObj instanceof ScriptableObject) {
            headers = (ScriptableObject) headersObj;
        }
        
        // extract body
        Object bodyObj = obj.get("body", obj);
        if (bodyObj instanceof String) {
            // be lenient and convert strings to arrays
            Object[] array = {(String) bodyObj};
            Context cx = CommonJSEngine.enterContext();
            try {
                bodyObj = cx.newArray(scope, array);
            } finally {
                Context.exit();
            }
        }
        if (bodyObj instanceof Scriptable) {
            body = (Scriptable) bodyObj;
            Object forEachObj = body.get("forEach", body);
            if (forEachObj instanceof Function) {
                forEach = (Function) forEachObj;
            } else {
                NativeArray bodyArray = null;
                if (body instanceof NativeArray) {
                    bodyArray = (NativeArray) body;
                }
                if (bodyArray != null) {
                    forEach = (Function) bodyArray.getPrototype().get("forEach", bodyArray);
                }
            }
        }
        
        if (forEach == null) {
            throw new RuntimeException("JSGI app must return an object with a 'body' member that has a 'forEach' function.");
        }

    }
    
    public void commit(Response response, final Scriptable scope) throws SecurityException, NoSuchMethodException {

        // set response status
        response.setStatus(new Status(status));
        
        // set response headers
        Form responseHeaders = (Form) response.getAttributes().get("org.restlet.http.headers");
        if (responseHeaders == null) {
            responseHeaders = new Form();
            response.getAttributes().put("org.restlet.http.headers", responseHeaders);
        }
        if (headers != null) {
            for (Object id : headers.getIds()) {
                String name = id.toString();
                String value = headers.get(name, headers).toString();
                if (!name.equalsIgnoreCase("content-type")) {
                    responseHeaders.add(name, value);
                }
            }
        }
        
        // write response body
        MediaType mediaType;
        String type = responseHeaders.getFirstValue("content-type", true);
        if (type == null) {
            mediaType = MediaType.TEXT_PLAIN;
        } else {
            mediaType = new MediaType(type);
        }
        
        final Method writeMethod = getClass().getDeclaredMethod("write", Context.class, Scriptable.class, Object[].class, Function.class);
        
        response.setEntity(new OutputRepresentation(mediaType) {
            
            @Override
            public void write(OutputStream outputStream) throws IOException {
                Context cx = CommonJSEngine.enterContext();
                FunctionObject writeFunc = new FunctionObject("bodyWriter", writeMethod, scope);
                BoundFunction boundWrite = new BoundFunction(cx, scope, writeFunc, body, new Object[] {outputStream});
                Object[] args = {boundWrite};
                try {
                    forEach.call(cx, scope, body, args);
                } finally {
                    Context.exit();
                    outputStream.close();
                }
            }
            
        });
    }
    
    public static Object write(Context cx, Scriptable thisObj, Object[] args, Function func) throws ScriptException {
        OutputStream outputStream = (OutputStream) args[0];
        Object part = args[1];
        byte[] bytes = null;
        if (part instanceof String) {
            bytes = ((String) part).getBytes();
        } else {
            LOGGER.severe("Unsupported response body type: " + part.toString());
        }
        if (bytes != null) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new ScriptException("Failed to write to body.");
            }
        }
        return null;
    }


}
