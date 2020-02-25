/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.service;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Map;
import org.geogig.geoserver.rest.GeoServerRepositoryProvider;
import org.geogig.geoserver.spring.dto.RepositoryImportRepo;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.plumbing.ResolveRepositoryName;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.locationtech.geogig.web.api.CommandSpecException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Internal service for importing a repository. */
@Service("importRepoService")
public class ImportRepoService {

    public RepositoryImportRepo importRepository(
            RepositoryProvider provider, String repositoryName, Map<String, String> parameters)
            throws RepositoryConnectionException {
        if (provider.hasGeoGig(repositoryName)) {
            throw new CommandSpecException(
                    "The specified repository name is already in use, please try a different name",
                    HttpStatus.CONFLICT);
        }

        if (!(provider instanceof GeoServerRepositoryProvider)) {
            throw new CommandSpecException("Unexpected repository provider");
        }
        Repository newRepo =
                ((GeoServerRepositoryProvider) provider)
                        .importExistingGeogig(repositoryName, parameters);

        if (newRepo == null) {
            throw new CommandSpecException("Repository not found", HttpStatus.NOT_FOUND);
        }

        Optional<URI> repoUri = newRepo.command(ResolveGeogigURI.class).call();
        Preconditions.checkState(
                repoUri.isPresent(), "Unable to resolve URI of imported repository.");

        final String repoName =
                RepositoryResolver.load(repoUri.get()).command(ResolveRepositoryName.class).call();
        RepositoryImportRepo info = new RepositoryImportRepo();
        info.setName(repoName);
        // set the Web API Atom Link, not the repository URI link
        info.setLink(RepositoryProvider.BASE_REPOSITORY_ROUTE + "/" + repoName);
        return info;
    }
}
