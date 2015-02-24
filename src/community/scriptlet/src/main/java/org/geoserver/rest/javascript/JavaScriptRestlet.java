/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.javascript;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.restlet.Restlet;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.StringRepresentation;

import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestletException;

public class JavaScriptRestlet extends Restlet {
    private String scriptPath;
    private GeoServerResourceLoader resourceLoader;
    private Catalog catalog;

    public void setResourceLoader(GeoServerResourceLoader loader) {
        resourceLoader = loader;
    }

    public void setRootPath(String s) { scriptPath = s; }

    public void setCatalog(Catalog c) { catalog = c; }

    public void handle(Request request, Response response) {
        String scriptName = (String) request.getAttributes().get("script");

        if (scriptName == null) {
            File scriptDirectory = null;
            try {
                scriptDirectory = resourceLoader.find(scriptPath);
            } catch (IOException ioe) {
                // no, it's cool. we might have to handle a null return anyway.
            }

            if (scriptDirectory == null) throw new RestletException(
                "No script directory", Status.CLIENT_ERROR_NOT_FOUND
            );

            StringBuilder out = new StringBuilder();
            for (String script : scriptDirectory.list(new FilenameFilter() {
                public boolean accept(File f, String name) {
                    return name.endsWith(".js");
                }
            })) {
                out.append(script.substring(0, script.length() - 3))
                   .append(", ");
            } 
            response.setEntity(new StringRepresentation(out.toString()));
        } else {
            File script;
            try {
                script = resourceLoader.find(scriptPath, scriptName + ".js");
            } catch (IOException ioe) {
                throw new RestletException(
                    "Requested script [" + scriptName + "] does not exist",
                    Status.CLIENT_ERROR_NOT_FOUND
                );
            }

            Context cx = Context.enter();
            try {
                Scriptable scope = cx.initStandardObjects();
                FileReader reader = new FileReader(script);

                Object wrappedRequest = Context.javaToJS(request, scope);
                Object wrappedResponse = Context.javaToJS(response, scope);
                Object wrappedCatalog = Context.javaToJS(catalog, scope);
                Object wrappedLoader = Context.javaToJS(resourceLoader, scope);

                ScriptableObject.putProperty(scope, "request", wrappedRequest);
                ScriptableObject
                    .putProperty(scope, "response", wrappedResponse);
                ScriptableObject.putProperty(scope, "loader", wrappedLoader);
                ScriptableObject.putProperty(scope, "catalog", wrappedCatalog);

                cx.evaluateReader(scope, reader, script.getName(), 1, null);
            } catch (IOException e) {
                throw new RestletException(
                    "I/O error while loading script...",
                    Status.SERVER_ERROR_INTERNAL
                );
            } finally {
                Context.exit();
            }
        }
    }
}
