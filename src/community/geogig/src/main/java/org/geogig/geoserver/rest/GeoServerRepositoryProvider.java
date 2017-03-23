/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.web.api.RESTUtils.getStringAttribute;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.locationtech.geogig.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.repository.impl.GeoGIG;
import org.locationtech.geogig.rest.RestletException;
import org.locationtech.geogig.rest.repository.InitRequestUtil;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Status;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;

/**
 * {@link RepositoryProvider} that looks up the coresponding {@link GeoGIG} instance to a given
 * {@link Request} by asking the geoserver's {@link RepositoryManager}
 */
public class GeoServerRepositoryProvider implements RepositoryProvider {

    /**
     * Init request command string.
     */
    public static final String INIT_CMD = "init";

    /**
     * Import Existing Repository command string.
     */
    public static final String IMPORT_CMD = "importExistingRepo";

    private Optional<String> getRepositoryName(Request request) {
        final String repo = getStringAttribute(request, "repository");
        return Optional.fromNullable(repo);
    }

    public Optional<RepositoryInfo> findRepository(Request request) {
        Optional<String> repositoryName = getRepositoryName(request);
        if (!repositoryName.isPresent()) {
            return Optional.absent();
        }
        try {
            String repoName = repositoryName.get();
            String repoId = getRepoIdForName(repoName);
            if (repoId != null) {
                RepositoryManager repositoryManager = RepositoryManager.get();
                RepositoryInfo repositoryInfo;
                repositoryInfo = repositoryManager.get(repoId);
                return Optional.of(repositoryInfo);
            } else {
                return Optional.absent();
            }
        } catch (NoSuchElementException | IOException e) {
            return Optional.absent();
        }
    }

    public List<RepositoryInfo> getRepositoryInfos() {
        return RepositoryManager.get().getAll();
    }

    private String getRepoIdForName(String repoName) {
        // get the list of Repos the Manager knows about
        // loop and return the id if we find one
        for (RepositoryInfo repo : getRepositoryInfos()) {
            if (repo.getRepoName().equals(repoName)) {
                return repo.getId();
            }
        }
        return null;
    }

    @Override
    public void delete(Request request) {
        Optional<Repository> geogig = getGeogig(request);
        Preconditions.checkState(geogig.isPresent(), "No repository to delete.");

        final String repositoryName = getStringAttribute(request, "repository");
        final String repoId = getRepoIdForName(repositoryName);
        Repository ggig = geogig.get();
        Optional<URI> repoUri = ggig.command(ResolveGeogigURI.class).call();
        Preconditions.checkState(repoUri.isPresent(), "No repository to delete.");

        ggig.close();
        try {
            GeoGIG.delete(repoUri.get());
            RepositoryManager manager = RepositoryManager.get();
            manager.delete(repoId);
        } catch (Exception e) {
            Throwables.propagate(e);
        }

    }

    @Override
    public void invalidate(String repoName) {
        final String repoId = getRepoIdForName(repoName);
        if (repoId != null) {
            RepositoryManager manager = RepositoryManager.get();
            manager.invalidate(repoId);
        }
    }

    @Override
    public Iterator<String> findRepositories() {
        List<RepositoryInfo> infos = getRepositoryInfos();
        return Iterators.transform(infos.iterator(), new Function<RepositoryInfo, String>() {
            @Override
            public String apply(RepositoryInfo input) {
                return input.getRepoName();
            }
        });
    }

    private boolean isInitRequest(Request request) {
        // if the request is a PUT, and the request path ends in "init", it's an INIT request.
        if (Method.PUT.equals(request.getMethod())) {
            Map<String, Object> attributes = request.getAttributes();
            if (attributes != null && attributes.containsKey("command")) {
                return INIT_CMD.equals(attributes.get("command"));
            } else if (request.getResourceRef() != null) {
                String path = request.getResourceRef().getPath();
                return path != null && path.contains(INIT_CMD);
            }
        }
        return false;
    }

    private boolean isImportRequest(Request request) {
        // if the request is a POST, and the request path ends in "importExistingRepo"
        if (Method.POST.equals(request.getMethod())) {
            Map<String, Object> attributes = request.getAttributes();
            if (attributes != null && attributes.containsKey("command")) {
                return IMPORT_CMD.equals(attributes.get("command"));
            } else if (request.getResourceRef() != null) {
                String path = request.getResourceRef().getPath();
                return path != null && path.contains(IMPORT_CMD);
            }
        }
        return false;
    }

    @Override
    public Optional<Repository> getGeogig(Request request) {
        Optional<String> repositoryName = getRepositoryName(request);
        if (!repositoryName.isPresent()) {
            return Optional.absent();
        }
        // look for one with the provided name first
        Optional<Repository> geogig = getGeogig(repositoryName.get());
        if (!geogig.isPresent()) {
            if (isInitRequest(request)) {
                // special handling of INIT requests
                geogig = AddRepoRequestHandler.createGeoGIG(request);
            } else if (isImportRequest(request)){
                // handles IMPORT requests
                geogig = AddRepoRequestHandler.importGeogig(request);
            }
        }
        if (!geogig.isPresent()) {
            // if it's still not present, just generate one.
            // This is so the CommandResource can get into the runCmd code before failing and
            // generating the correct responses
            geogig = Optional.fromNullable(RepositoryManager.get().createRepo(new Hints()));
        }
        return geogig;
    }

    public Optional<Repository> getGeogig(String repositoryName) {
        Repository geogig = findRepository(repositoryName);
        return Optional.fromNullable(geogig);
    }

    private Repository findRepository(String repositoryName) {

        RepositoryManager manager = RepositoryManager.get();
        String repoId = getRepoIdForName(repositoryName);
        if (null == repoId) {
            return null;
        }
        try {
            RepositoryInfo info = manager.get(repoId);
            return manager.getRepository(repoId);
        } catch (IOException e) {
            throw new RestletException("Error accessing datastore " + repositoryName,
                    Status.SERVER_ERROR_INTERNAL, e);
        }
    }

    private static class AddRepoRequestHandler {

        private static Optional<Repository> createGeoGIG(Request request) {
            try {
                final Hints hints = InitRequestUtil.createHintsFromRequest(request);
                // now build the repo with the Hints
                return Optional.fromNullable(RepositoryManager.get().createRepo(hints));
            } catch (Exception ex) {
                Throwables.propagate(ex);
            }
            return Optional.absent();
        }

        private static Optional<Repository> importGeogig(Request request) {

            try {
                // build URI
                Hints hint = InitRequestUtil.createHintsFromRequest(request);

                // now build the repo with the Hints
                RepositoryInfo repoInfo = new RepositoryInfo();

                // set the repo location from the URI
                if (!hint.get(Hints.REPOSITORY_URL).isPresent()) {
                	return Optional.absent();
                }
                URI uri = new URI(hint.get(Hints.REPOSITORY_URL).get().toString());
                repoInfo.setLocation(uri);

                // check to see if repo is initialized
                RepositoryResolver repoResolver = RepositoryResolver.lookup(uri);
                if(!repoResolver.repoExists(uri)) {
                    return Optional.absent();
                }

                // save the repo, this will set a UUID
                RepositoryManager.get().save(repoInfo);

                return Optional.of(RepositoryManager.get().getRepository(repoInfo.getId()));
            } catch (IOException | URISyntaxException e) {
                Throwables.propagate(e);
            } catch (RepositoryConnectionException e) {
                e.printStackTrace();
            }
            return Optional.absent();
        }
    }
}
