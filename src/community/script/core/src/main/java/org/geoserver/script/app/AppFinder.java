/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.app;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.rest.RestletException;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.rest.FinderSupport;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * Finder for app scripts.
 * <p>
 * Handles requests of the form /apps/[<app>].
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class AppFinder extends FinderSupport {

    public AppFinder(ScriptManager scriptMgr) {
        super(scriptMgr);
    }
   
    @Override
    protected Resource doFindTarget(Request request, Response response) {
        String app = (String) request.getAttributes().get("app");

        if (app == null) {
            return new AppListResource(scriptMgr, request, response);
        }

        File appDir;
        try {
            appDir = scriptMgr.findAppDir(app);
        } catch (IOException e) {
            throw new RestletException(format("Error looking up app directory %s", app), 
                Status.SERVER_ERROR_INTERNAL, e);
        }

        if (appDir == null) {
            throw new RestletException(format("No such app %s", app), Status.CLIENT_ERROR_NOT_FOUND);
        }

        //look for main script
        File main = scriptMgr.findAppMainScript(appDir);
        if (main == null) {
            throw new RestletException(format("No main file for app %s", app), Status.CLIENT_ERROR_NOT_FOUND);
        }

        return new AppResource(main, scriptMgr, request, response);
    }
}
