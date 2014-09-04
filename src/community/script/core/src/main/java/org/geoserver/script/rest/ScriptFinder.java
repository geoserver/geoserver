/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.rest;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.script.ScriptManager;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * Finder for {@linnk ScriptResource} instances.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class ScriptFinder extends FinderSupport {

    public ScriptFinder(ScriptManager scriptMgr) {
        super(scriptMgr);
    }

    @Override
    protected Resource doFindTarget(Request request, Response response) {
        String name = (String) request.getAttributes().get("name");

        //get a relative reference
        String path = request.getResourceRef().getRelativeRef(request.getRootRef()).getPath();
        path = ResponseUtils.stripBeginningPath(path);
        
        if (name != null) {
            // direct script
            return new ScriptResource(scriptMgr, path, request, response);
        }
        else {
            // collection of scripts
            return new ScriptListResource(scriptMgr, stripExtension(path), request, response);
        }
    }
    
    private String stripExtension(String path) {
        int i = path.lastIndexOf(".");
        if (i > -1) {
            return path.substring(0, i);
        } else {
            return path;
        }
    }
}
