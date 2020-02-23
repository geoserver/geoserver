/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JsgiRequest extends ScriptableObject {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    static Logger LOGGER = Logging.getLogger("org.geoserver.script.js");

    /**
     * Generates a JavaScript object that conforms to the JSGI spec.
     * http://wiki.commonjs.org/wiki/JSGI/Level0/A/Draft2
     *
     */
    public JsgiRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            Context cx,
            Scriptable scope) {
        super(scope, ScriptableObject.getObjectPrototype(scope));

        this.put("method", this, request.getMethod().toString());

        this.put(
                "scriptName",
                this,
                request.getPathInfo()
                        .substring(
                                request.getPathInfo().lastIndexOf("/"))); /*ref.getLastSegment());*/

        this.put(
                "pathInfo", this, request.getPathInfo()); /*StringUtils.join(seg.toArray(), "/"));*/

        this.put("queryString", this, request.getQueryString()); /*ref.getQuery());*/

        this.put("host", this, request.getContextPath()); /*ref.getHostDomain());*/

        this.put("port", this, request.getServerPort()); /*ref.getHostPort());*/

        this.put("scheme", this, request.getScheme()); /*ref.getScheme());*/

        try {
            // TODO: deal with input
            this.put("input", this, Context.javaToJS(request.getInputStream(), scope));
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }

        Scriptable headers = cx.newObject(scope);
        Enumeration<String> requestHeaders = request.getHeaderNames();
        while (requestHeaders.hasMoreElements()) {
            String value = request.getHeader(requestHeaders.nextElement());
            headers.put(requestHeaders.nextElement().toLowerCase(), headers, value);
        }

        this.put("headers", this, headers);

        // create jsgi object
        Scriptable jsgiObject = cx.newObject(scope);
        int readonly = ScriptableObject.PERMANENT | ScriptableObject.READONLY;
        Scriptable version =
                cx.newArray(scope, new Object[] {Integer.valueOf(0), Integer.valueOf(3)});
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
