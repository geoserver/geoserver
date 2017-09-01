/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import java.io.File;
import java.net.URI;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.locationtech.geogig.web.api.CommandSpecException;
import org.locationtech.geogig.web.api.RESTUtils;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class InitCommandResource extends org.locationtech.geogig.rest.repository.InitCommandResource {
    
    @Override
    protected Representation runCommand(Variant variant, Request request, MediaType outputFormat) {
        // before running the Init command, extract the repository name from the request and see if
        // a repository with that name already exists
        final String repoName = RESTUtils.getStringAttribute(request, "repository");
        if (repoName != null && RepositoryManager.get().repoExistsByName(repoName)) {
            // repo already exists
            throw new CommandSpecException(
                    "The specified repository name is already in use, please try a different name",
                    Status.CLIENT_ERROR_CONFLICT);
        }
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
