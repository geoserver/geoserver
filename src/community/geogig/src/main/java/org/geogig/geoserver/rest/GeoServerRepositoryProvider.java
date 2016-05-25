/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.repository.RESTUtils.getStringAttribute;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.geogig.geoserver.config.ConfigStore;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestletException;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.api.plumbing.ResolveGeogigURI;
import org.locationtech.geogig.repository.Hints;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
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
    
    private Map<String, String> repoNameToId = new HashMap<String, String>();

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

    @Override
    public Optional<GeoGIG> getGeogig(Request request) {
        Optional<String> repositoryName = getRepositoryName(request);
        if (!repositoryName.isPresent()) {
            return Optional.absent();
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
        if (repoId == null) {
        	repoId = UUID.randomUUID().toString();
        }
        try {
            RepositoryInfo info = manager.get(repoId);
            return manager.getRepository(repoId);
        } catch (NoSuchElementException e) {
            Hints hints = new Hints();
            hints.set(Hints.REPOSITORY_NAME, repositoryName);
            return manager.createRepo(hints, repoId);
        } catch (IOException e) {
            throw new RestletException("Error accessing datastore " + repositoryName,
                    Status.SERVER_ERROR_INTERNAL, e);
        }
    }

}