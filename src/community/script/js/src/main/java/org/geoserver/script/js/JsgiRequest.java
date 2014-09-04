/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class JsgiRequest extends ScriptableObject {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    static Logger LOGGER = Logging.getLogger("org.geoserver.script.js");

    /**
     * Generates a JavaScript object that conforms to the JSGI spec.
     * http://wiki.commonjs.org/wiki/JSGI/Level0/A/Draft2
     * 
     * @param request
     * @param response
     * @param cx
     * @param scope
     */
    public JsgiRequest(Request request, Response response, Context cx, Scriptable scope) {
        super(scope, ScriptableObject.getObjectPrototype(scope));

        Reference ref = request.getResourceRef();

        this.put("method", this, request.getMethod().toString());

        this.put("scriptName", this, ref.getLastSegment());

        List<String> seg = new ArrayList<String>(ref.getSegments().subList(4, ref.getSegments().size()));
        seg.add(0, "");
        this.put("pathInfo", this, StringUtils.join(seg.toArray(), "/"));
        
        this.put("queryString", this, ref.getQuery());
        
        this.put("host", this, ref.getHostDomain());
        
        this.put("port", this, ref.getHostPort());
        
        this.put("scheme", this, ref.getScheme());

        try {
            // TODO: deal with input
            this.put("input", this, Context.javaToJS(request.getEntity().getStream(), scope));
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }

        Scriptable headers = cx.newObject(scope);
        Form requestHeaders = (Form) request.getAttributes().get("org.restlet.http.headers");
        for (String name : requestHeaders.getNames()) {
            String value = requestHeaders.getFirstValue(name, true);
            headers.put(name.toLowerCase(), headers, value);
        }
        this.put("headers", this, headers);

        // create jsgi object
        Scriptable jsgiObject = cx.newObject(scope);
        int readonly = ScriptableObject.PERMANENT | ScriptableObject.READONLY;
        Scriptable version = cx.newArray(scope, new Object[] {Integer.valueOf(0), Integer.valueOf(3)});
        ScriptableObject.defineProperty(jsgiObject, "version", version, readonly);
        ScriptableObject.defineProperty(jsgiObject, "multithread", Boolean.TRUE, readonly);
        ScriptableObject.defineProperty(jsgiObject, "multiprocess", Boolean.FALSE, readonly);
        ScriptableObject.defineProperty(jsgiObject, "async", Boolean.TRUE, readonly);
        ScriptableObject.defineProperty(jsgiObject, "runOnce", Boolean.FALSE, readonly);
        ScriptableObject.defineProperty(jsgiObject, "cgi", Boolean.FALSE, readonly);
        this.put("jsgi", this, jsgiObject);

        // create env object
        Scriptable env = cx.newObject(scope);
        env.put("servletRequest", env, Context.javaToJS(request, scope));
        env.put("servletResponse", env, Context.javaToJS(response, scope));
        this.put("env", this, env);
        
    }

    @Override
    public String getClassName() {
        return "JsgiRequest";
    }

}
