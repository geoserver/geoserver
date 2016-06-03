/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.repository.RESTUtils.getStringAttribute;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.rest.RestletException;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.api.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.restlet.data.Form;
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
     * Init request form field for specifying the Repository URI.
     */
    public static final String REPO_URI_KEY = "repositoryURI";

    private final Map<String, String> repoNameToId = new HashMap<>();

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
    
    private void updateMappings() {
    	List<RepositoryInfo> infos = getRepositoryInfos();
    	repoNameToId.clear();
    	for (RepositoryInfo info : infos) {
    		repoNameToId.put(info.getRepoName(), info.getId());
    	}
    }
    
    private String getRepoIdForName(String repoName) {
        String repoId = repoNameToId.get(repoName);
        if (repoId == null) {
        	updateMappings();
        	repoId = repoNameToId.get(repoName);
        }
        return repoId;
    }

    @Override
    public void delete(Request request) {
        Optional<GeoGIG> geogig = getGeogig(request);
        Preconditions.checkState(geogig.isPresent(), "No repository to delete.");

        final String repositoryName = getStringAttribute(request, "repository");
        final String repoId = repoNameToId.get(repositoryName);
        GeoGIG ggig = geogig.get();
        Optional<URI> repoUri = ggig.command(ResolveGeogigURI.class).call();
        Preconditions.checkState(repoUri.isPresent(), "No repository to delete.");

        ggig.close();
        try {
            GeoGIG.delete(repoUri.get());
            RepositoryManager manager = RepositoryManager.get();
            manager.invalidate(repoId);
            repoNameToId.remove(repositoryName);
        } catch (Exception e) {
            Throwables.propagate(e);
        }

    }

    @Override
    public void invalidate(String repoName) {
        if (repoNameToId.containsKey(repoName)) {
            final String repoId = repoNameToId.get(repoName);
            RepositoryManager manager = RepositoryManager.get();
            manager.invalidate(repoId);
            repoNameToId.remove(repoName);
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
        return Method.PUT.equals(request.getMethod()) && request.getResourceRef() != null &&
                request.getResourceRef().getPath().endsWith(INIT_CMD);
    }

    private Optional<URI> getRepositoryURI(Request request) {
        // look for a repositoryURI in the From
        if (request.isEntityAvailable()) {
            try {
                Form requestForm = request.getEntityAsForm();
                String repoURI;
                if (requestForm != null &&
                        (repoURI = requestForm.getFirstValue(REPO_URI_KEY, null)) != null) {
                    // the request has a URI in the form
                    return Optional.of(URI.create(repoURI));
                }
            } catch (Exception ex) {
                // eat it
                // the request entity is not a form, or there is no form in the request
            }
        }
        // no URI specified in the request form
        return Optional.absent();
    }

    private Optional<GeoGIG> getGeogig(URI uri, String repositoryName) {
        Hints hints = new Hints();
        hints.set(Hints.REPOSITORY_URL, uri.normalize());
        hints.set(Hints.REPOSITORY_NAME, repositoryName);
        return Optional.of(RepositoryManager.get().createRepo(hints));
    }

    @Override
    public Optional<GeoGIG> getGeogig(Request request) {
        Optional<String> repositoryName = getRepositoryName(request);
        if (!repositoryName.isPresent()) {
            return Optional.absent();
        }
        // special handling of INIT requests
        // if the request is an INIT request and has a repositoryURI in the request, use that
        if (isInitRequest(request)) {
            Optional<URI> uri = getRepositoryURI(request);
            if (uri.isPresent()) {
                return getGeogig(uri.get(), repositoryName.get());
            }
        }
        return getGeogig(repositoryName.get());
    }
    
    public Optional<GeoGIG> getGeogig(String repositoryName) {
        GeoGIG geogig = findRepository(repositoryName);
        return Optional.of(geogig);
    }

    private GeoGIG findRepository(String repositoryName) {

        RepositoryManager manager = RepositoryManager.get();
        String repoId = getRepoIdForName(repositoryName);
        if (null == repoId) {
            // this should only be the case if we are processing an INIT request, with no request
            // entity. Create a repo with just the name specified by the user.
            Hints hints = new Hints();
            hints.set(Hints.REPOSITORY_NAME, repositoryName);
            return manager.createRepo(hints);
        }
        try {
            RepositoryInfo info = manager.get(repoId);
            return manager.getRepository(repoId);
        } catch (IOException e) {
            throw new RestletException("Error accessing datastore " + repositoryName,
                    Status.SERVER_ERROR_INTERNAL, e);
        }
    }

}