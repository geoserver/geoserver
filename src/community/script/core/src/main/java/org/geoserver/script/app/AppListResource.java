/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.app;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import net.sf.json.JSONArray;

import org.geoserver.rest.RestletException;
import org.geoserver.script.ScriptManager;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;

/**
 * Resource that lists all available apps as a JSON array of strings. 
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class AppListResource extends Resource {

    ScriptManager scriptMgr;

    public AppListResource(ScriptManager scriptMgr, Request request, Response response) {
        super(null, request, response);
        this.scriptMgr = scriptMgr;
    }

    public void handleGet() {
        File appRoot;
        try {
            appRoot = scriptMgr.getAppRoot();
        } catch (IOException e) {
            throw new RestletException("Error looking up apps", Status.SERVER_ERROR_INTERNAL, e);
        }

        File[] apps = appRoot.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        JSONArray array = new JSONArray();
        for (File f : apps) {
            array.add(f.getName());
        }

        getResponse().setEntity(new StringRepresentation(array.toString(), MediaType.APPLICATION_JSON));
        getResponse().setStatus(Status.SUCCESS_OK);
    };
}
