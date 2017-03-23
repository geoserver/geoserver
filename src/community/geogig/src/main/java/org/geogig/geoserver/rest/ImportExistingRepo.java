package org.geogig.geoserver.rest;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.plumbing.ResolveRepositoryName;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.locationtech.geogig.web.api.*;
import org.restlet.data.Method;
import org.restlet.data.Status;

import java.net.URI;

public class ImportExistingRepo extends AbstractWebAPICommand {

	@Override
	protected void setParametersInternal(ParameterSet options) {
	}

    @Override
    public boolean supports(final Method method) {
        return Method.POST.equals(method);
    }

    @Override
    protected boolean requiresOpenRepo() {
        return true;
    }

    @Override
    public boolean requiresTransaction() {
        return false;
    }

    /**
     * Runs the command and builds the appropriate response
     *
     * @param context - the context to use for this command
     *
     * @throws CommandSpecException
     */
    @Override
    protected void runInternal(CommandContext context) {

        final Context geogig = this.getRepositoryContext(context);
        Optional<URI> repoUri = geogig.command(ResolveGeogigURI.class).call();
        Preconditions.checkState(repoUri.isPresent(),
                "Unable to resolve URI of imported repository.");

        try {
            final String repositoryName = RepositoryResolver.load(repoUri.get())
                    .command(ResolveRepositoryName.class).call();
            context.setResponseContent(new CommandResponse() {
                @Override
                public void write(ResponseWriter out) throws Exception {
                    out.start();
                    out.writeRepoInitResponse(repositoryName, context.getBaseURL(),
                            RepositoryProvider.BASE_REPOSITORY_ROUTE + "/" + repositoryName);
                    out.finish();
                }
            });
            // repo was created successfully
            setStatus(Status.SUCCESS_OK);
        } catch (RepositoryConnectionException e) {
            throw new CommandSpecException(
                    "Repository was imported, but was unable to connect to it immediately.");
        }
    }
}