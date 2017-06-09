package org.geogig.geoserver.rest;

import org.geogig.geoserver.config.RepositoryManager;
import org.locationtech.geogig.rest.repository.CommandResource;
import org.locationtech.geogig.web.api.CommandSpecException;
import org.locationtech.geogig.web.api.ParameterSet;
import org.locationtech.geogig.web.api.RESTUtils;
import org.locationtech.geogig.web.api.WebAPICommand;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

public class ImportRepoCommandResource extends CommandResource {

    @Override
    protected String getCommandName() {
        return "importExistingRepo";
    }
    
    @Override
    protected ParameterSet handleRequestEntity(Request request) {
    	// the request handler will take care of the entity.
    	return null;
    }

    @Override
    protected WebAPICommand buildCommand(String commandName) {
        return new ImportExistingRepo();
    }

    @Override
    protected Representation runCommand(Variant variant, Request request, MediaType outputFormat) {
        // before importing the repository, extract the repository name from the request and see if
        // a repository with that name already exists
        final String repoName = RESTUtils.getStringAttribute(request, "repository");
        if (repoName != null) {
            if (RepositoryManager.get().getByRepoName(repoName) != null) {
                // repo already exists
                throw new CommandSpecException(
                    "The specified repository name is already in use, please try a different name",
                    Status.CLIENT_ERROR_CONFLICT);
            }
        }
        // all good
        return super.runCommand(variant, request, outputFormat);
    }
}