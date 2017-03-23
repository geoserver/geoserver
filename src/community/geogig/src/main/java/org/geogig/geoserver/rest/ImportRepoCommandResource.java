package org.geogig.geoserver.rest;

import org.locationtech.geogig.rest.repository.CommandResource;
import org.locationtech.geogig.web.api.ParameterSet;
import org.locationtech.geogig.web.api.WebAPICommand;
import org.restlet.data.Request;

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
}