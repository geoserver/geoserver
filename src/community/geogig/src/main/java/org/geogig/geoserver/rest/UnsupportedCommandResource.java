package org.geogig.geoserver.rest;

import org.locationtech.geogig.rest.repository.CommandResource;
import org.locationtech.geogig.web.api.AbstractWebAPICommand;
import org.locationtech.geogig.web.api.CommandContext;
import org.locationtech.geogig.web.api.CommandSpecException;
import org.locationtech.geogig.web.api.ParameterSet;
import org.locationtech.geogig.web.api.WebAPICommand;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Status;

public class UnsupportedCommandResource extends CommandResource {
    
    @Override
    protected ParameterSet handleRequestEntity(Request request) {
        return null;
    }

    @Override
    protected WebAPICommand buildCommand(String commandName) {
        return new UnsupportedCommand();
    }
    
    class UnsupportedCommand extends AbstractWebAPICommand {

        @Override
        protected void setParametersInternal(ParameterSet options) {
        }

        @Override
        public boolean supports(final Method method) {
            return true;
        }

        @Override
        protected boolean requiresOpenRepo() {
            return false;
        }

        @Override
        public boolean requiresTransaction() {
            return false;
        }

        /**
         * @throws CommandSpecException to inform user of unsupported command
         */
        @Override
        protected void runInternal(CommandContext context) {        	
            throw new CommandSpecException("This command is unsupported by the GeoGig plugin.", Status.CLIENT_ERROR_BAD_REQUEST);
        }
	
    }
}