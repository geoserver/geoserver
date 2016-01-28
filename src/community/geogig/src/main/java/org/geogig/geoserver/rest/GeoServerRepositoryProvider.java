/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.repository.RESTUtils.getStringAttribute;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.rest.RestletException;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.restlet.data.Request;
import org.restlet.data.Status;

import com.google.common.base.Optional;

/**
 * {@link RepositoryProvider} that looks up the coresponding {@link GeoGIG} instance to a given
 * {@link Request} by asking the geoserver's {@link RepositoryManager}
 */
public class GeoServerRepositoryProvider implements RepositoryProvider {

    private Optional<String> getRepositoryId(Request request) {
        final String repo = getStringAttribute(request, "repository");
        return Optional.fromNullable(repo);
    }

    public Optional<RepositoryInfo> findRepository(Request request) {
        Optional<String> repositoryId = getRepositoryId(request);
        if (!repositoryId.isPresent()) {
            return Optional.absent();
        }
        try {
            String repoId = repositoryId.get();
            RepositoryManager repositoryManager = RepositoryManager.get();
            RepositoryInfo repositoryInfo;
            repositoryInfo = repositoryManager.get(repoId);
            return Optional.of(repositoryInfo);
        } catch (NoSuchElementException | IOException e) {
            return Optional.absent();
        }
    }

    public List<RepositoryInfo> findRepositories() {
        return RepositoryManager.get().getAll();
    }

    @Override
    public Optional<GeoGIG> getGeogig(Request request) {
        Optional<String> repositoryId = getRepositoryId(request);
        if (!repositoryId.isPresent()) {
            return Optional.absent();
        }
        GeoGIG geogig = findRepository(request, repositoryId.get());
        return Optional.of(geogig);
    }

    private GeoGIG findRepository(Request request, String repositoryId) {

        RepositoryManager manager = RepositoryManager.get();
        try {
            manager.get(repositoryId);
            return manager.getRepository(repositoryId);
        } catch (NoSuchElementException e) {
            throw new RestletException("No such repository: " + repositoryId,
                    Status.CLIENT_ERROR_NOT_FOUND);
        } catch (IOException e) {
            throw new RestletException("Error accessing datastore " + repositoryId,
                    Status.SERVER_ERROR_INTERNAL, e);
        }
    }

}