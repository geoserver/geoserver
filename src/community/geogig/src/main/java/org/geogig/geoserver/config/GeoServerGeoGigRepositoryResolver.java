/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import java.io.IOException;
import java.net.URI;

import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.GeoGIG;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.RepositoryResolver;
import org.locationtech.geogig.storage.ConfigDatabase;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/**
 * Specialized RepositoryResolver for GeoServer manager Geogig Repositories.
 */
public class GeoServerGeoGigRepositoryResolver extends RepositoryResolver {

    public static final String GEOSERVER_URI_SCHEME = "geoserver";
    public static final int SCHEME_LENGTH = GEOSERVER_URI_SCHEME.length() + "://".length();

    public static String getURI(String repoName) {
        return String.format("%s://%s", GEOSERVER_URI_SCHEME, repoName);
    }

    @Override
    public boolean canHandle(URI repoURI) {
        return repoURI != null && GEOSERVER_URI_SCHEME.equals(repoURI.getScheme());
    }

    @Override
    public boolean repoExists(URI repoURI) throws IllegalArgumentException {
        String name = getName(repoURI);
        RepositoryManager repoMgr = RepositoryManager.get();
        // get the repo by name
        RepositoryInfo repoInfo;
        try {
            repoInfo = repoMgr.getByRepoName(name);
            // if it doesn't throw an exception, it exists
            return repoInfo != null;
        } catch (Exception ex) {
            // doesn't exist
            return false;
        }
    }

    @Override
    public String getName(URI repoURI) {
        Preconditions.checkArgument(canHandle(repoURI), "Not a GeoServer GeoGig repository URI: %s",
                repoURI);
        // valid looking URI, strip the name part out and get everything after the scheme
        // "geoserver" and the "://"
        String name = repoURI.toString().substring(SCHEME_LENGTH);
        // if it's empty, they didn't provide a name or Id
        Preconditions.checkArgument(!Strings.isNullOrEmpty(name),
                "No GeoGig repository Name or ID specified");
        return name;
    }

    @Override
    public void initialize(URI repoURI, Context repoContext) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ConfigDatabase getConfigDatabase(URI repoURI, Context repoContext) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Repository open(URI repositoryLocation) throws RepositoryConnectionException {
        String name = getName(repositoryLocation);
        // get a handle to the RepositoryManager
        RepositoryManager repoMgr = RepositoryManager.get();
        // get the repo by name
        try {
            RepositoryInfo info = repoMgr.getByRepoName(name);
            String repositoryId = info.getId();
            GeoGIG gig = repoMgr.getRepository(repositoryId);
            Repository repo = gig.getRepository();
            if (!repo.isOpen()) {
                repo.open();
            }
            return repo;
        } catch (IOException ioe) {
            // didn't find a repo
            RepositoryConnectionException rce = new RepositoryConnectionException(
                    "No GeoGig repository found with NAME or ID: " + name);
            rce.initCause(ioe);
            throw rce;
        }
    }

    @Override
    public boolean delete(URI repositoryLocation) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
