/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.service;

import static org.locationtech.geogig.porcelain.ConfigOp.ConfigAction.CONFIG_SET;
import static org.locationtech.geogig.porcelain.ConfigOp.ConfigScope.LOCAL;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.io.File;
import java.net.URI;
import java.util.Map;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.plumbing.ResolveRepositoryName;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.InitOp;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.locationtech.geogig.spring.dto.InitRequest;
import org.locationtech.geogig.spring.dto.RepositoryInitRepo;
import org.locationtech.geogig.spring.service.RepositoryInitService;
import org.locationtech.geogig.web.api.CommandSpecException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Replace the default {@link RepositoryInitService} with one that saves the repository info. */
@Service("repositoryInitService")
public class GeoserverRepositoryInitService extends RepositoryInitService {

    @Override
    public RepositoryInitRepo initRepository(
            RepositoryProvider provider, String repositoryName, Map<String, String> parameters)
            throws RepositoryConnectionException {
        if (provider.hasGeoGig(repositoryName)) {
            throw new CommandSpecException(
                    "The specified repository name is already in use, please try a different name",
                    HttpStatus.CONFLICT);
        }

        Repository newRepo = provider.createGeogig(repositoryName, parameters);

        if (newRepo.isOpen()) {
            throw new CommandSpecException(
                    "Cannot run init on an already initialized repository.", HttpStatus.CONFLICT);
        }

        InitOp command = newRepo.command(InitOp.class);

        newRepo = command.call();

        // set author inof, if provided in request parameters
        String authorName = parameters.get(InitRequest.AUTHORNAME);
        String authorEmail = parameters.get(InitRequest.AUTHOREMAIL);
        if (authorName != null || authorEmail != null) {
            ConfigOp configOp = newRepo.command(ConfigOp.class);
            configOp.setAction(CONFIG_SET)
                    .setScope(LOCAL)
                    .setName("user.name")
                    .setValue(authorName)
                    .call();
            configOp.setAction(CONFIG_SET)
                    .setScope(LOCAL)
                    .setName("user.email")
                    .setValue(authorEmail)
                    .call();
        }
        Optional<URI> repoUri = newRepo.command(ResolveGeogigURI.class).call();
        Preconditions.checkState(
                repoUri.isPresent(), "Unable to resolve URI of newly created repository.");

        final String repoName =
                RepositoryResolver.load(repoUri.get()).command(ResolveRepositoryName.class).call();
        RepositoryInitRepo info = new RepositoryInitRepo();
        info.setName(repoName);
        // set the Web API Atom Link, not the repository URI link
        info.setLink(RepositoryProvider.BASE_REPOSITORY_ROUTE + "/" + repoName);
        saveRepository(newRepo);
        return info;
    }

    private RepositoryInfo saveRepository(Repository geogig) {
        // repo was just created, need to register it with an ID in the manager
        // create a RepositoryInfo object
        RepositoryInfo repoInfo = new RepositoryInfo();
        URI location = geogig.getLocation().normalize();
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
