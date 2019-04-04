/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import java.net.URI;
import java.util.List;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.storage.ConfigDatabase;

/** Specialized RepositoryResolver for GeoServer manager Geogig Repositories. */
public class GeoServerGeoGigRepositoryResolver extends RepositoryResolver {

    public static final String GEOSERVER_URI_SCHEME = "geoserver";

    public static final int SCHEME_LENGTH = GEOSERVER_URI_SCHEME.length() + "://".length();

    public static String getURI(String repoName) {
        return String.format("%s://%s", GEOSERVER_URI_SCHEME, repoName);
    }

    @Override
    public boolean canHandle(URI repoURI) {
        return repoURI != null && canHandleURIScheme(repoURI.getScheme());
    }

    @Override
    public boolean canHandleURIScheme(String scheme) {
        return scheme != null && GEOSERVER_URI_SCHEME.equals(scheme);
    }

    @Override
    public boolean repoExists(URI repoURI) throws IllegalArgumentException {
        String name = getName(repoURI);
        RepositoryManager repoMgr = RepositoryManager.get();
        // get the repo by name
        RepositoryInfo repoInfo = repoMgr.getByRepoName(name);
        return repoInfo != null;
    }

    @Override
    public String getName(URI repoURI) {
        checkArgument(canHandle(repoURI), "Not a GeoServer GeoGig repository URI: %s", repoURI);
        // valid looking URI, strip the name part out and get everything after the scheme
        // "geoserver" and the "://"
        String name = repoURI.toString().substring(SCHEME_LENGTH);
        // if it's empty, they didn't provide a name or Id
        checkArgument(!Strings.isNullOrEmpty(name), "No GeoGig repository Name or ID specified");
        return name;
    }

    @Override
    public void initialize(URI repoURI, Context repoContext) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ConfigDatabase getConfigDatabase(URI repoURI, Context repoContext, boolean rootUri) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Repository open(URI repositoryLocation) throws RepositoryConnectionException {
        String name = getName(repositoryLocation);
        // get a handle to the RepositoryManager
        RepositoryManager repoMgr = RepositoryManager.get();
        // get the repo by name
        RepositoryInfo info = repoMgr.getByRepoName(name);
        if (info != null) {
            // get the native RepositoryResolver for the location and open it directly
            // Using the RepositryManager to get the repo would cause the repo to be managed by the
            // RepositoryManager,
            // when this repo should be managed by the DataStore. The DataStore will close this repo
            // instance when
            // GeoServer decides to dispose the DataStore.
            Repository repo = RepositoryResolver.load(info.getLocation());
            checkState(
                    repo.isOpen(), "RepositoryManager returned a closed repository for %s", name);
            return repo;
        } else {
            // didn't find a repo
            RepositoryConnectionException rce =
                    new RepositoryConnectionException(
                            "No GeoGig repository found with NAME or ID: " + name);
            throw rce;
        }
    }

    @Override
    public boolean delete(URI repositoryLocation) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public URI buildRepoURI(URI rootRepoURI, String repoName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> listRepoNamesUnderRootURI(URI rootRepoURI) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
