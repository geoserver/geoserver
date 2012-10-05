/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.rest;

import static java.lang.String.format;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import net.sf.json.JSONArray;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.rest.RestletException;
import org.geoserver.script.ScriptManager;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;

/**
 * Resource that lists all available scripts as a JSON array of strings. 
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class ScriptListResource extends Resource {

    ScriptManager scriptMgr;
    String path;
    
    public ScriptListResource(ScriptManager scriptMgr, String path, Request request, Response response) {
        super(null, request, response);
        this.scriptMgr = scriptMgr;
        this.path = path;
    }

    @Override
    public void handleGet() {
        final String type = (String) getRequest().getAttributes().get("type");

        File dir = null;
        try {
            dir = scriptMgr.findScriptDir(path);
        } catch (IOException e) {
            throw new RestletException(format("Error looking up script dir %s", path),
                Status.SERVER_ERROR_INTERNAL, e);
        }


        JSONArray arr = new JSONArray();
        if (dir != null) {
            FileFilter filter = type != null ? 
                new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return type.equalsIgnoreCase(FilenameUtils.getExtension(pathname.getName()));
                    }
                } :
                new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return true;
                    }
                };
            for (File f : dir.listFiles(filter)) {
                arr.add(f.getName());
            }
        }
        else {
            //return empty array, perhaps we should return a 404?
            //throw new RestletException(format("Could not find script dir %s", path), 
            //    Status.CLIENT_ERROR_NOT_FOUND);
        }

        getResponse().setEntity(new StringRepresentation(arr.toString()));
        getResponse().setStatus(Status.SUCCESS_OK);
    }

}
