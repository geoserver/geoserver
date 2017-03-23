/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import java.io.File;
import java.net.URI;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.locationtech.geogig.web.api.ParameterSet;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class InitCommandResource extends org.locationtech.geogig.rest.repository.InitCommandResource {

    @Override
    protected String getCommandName() {
        return "init";
    }

    @Override
    protected ParameterSet handleRequestEntity(Request request) {
    	// the request handler will take care of the entity.
    	return null;
    }
    
    @Override
    protected Representation runCommand(Variant variant, Request request, MediaType outputFormat) {
        Representation representation = super.runCommand(variant, request, outputFormat);

        if (getResponse().getStatus() == Status.SUCCESS_CREATED) {
            // save the repo in the Manager
            saveRepository();
        }
        return representation;
    }

    private RepositoryInfo saveRepository() {
        // repo was just created, need to register it with an ID in the manager
        // create a RepositoryInfo object
        RepositoryInfo repoInfo = new RepositoryInfo();
        URI location = geogig.get().getLocation().normalize();
        if ("file".equals(location.getScheme())) {
            // need the parent
            File parentDir = new File(location).getParentFile();
            location = parentDir.toURI().normalize();
        }
        // set the URI
        repoInfo.setLocation(location);
        // save the repo, this will set a UUID
        return RepositoryManager.get().save(repoInfo);
    }
}
