package org.geogig.geoserver.rest;

import org.locationtech.geogig.rest.repository.CommandResource;
import org.locationtech.geogig.web.api.ParameterSet;
import org.locationtech.geogig.web.api.WebAPICommand;

public class ImportRepoCommandResource extends CommandResource {

    @Override
    protected String getCommandName() {
        return "importExistingRepo";
    }

    @Override
    protected WebAPICommand buildCommand(String commandName, ParameterSet options) {
        return new ImportExistingRepo(options);
    }
}