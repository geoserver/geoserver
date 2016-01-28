/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import java.io.IOException;
import java.net.URI;

import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

import com.google.common.base.Throwables;
import java.io.File;

public class GeoServerStoreRepositoryResolver implements GeoGigDataStoreFactory.RepositoryLookup {

    @Override
    public URI resolve(final String repository) {
        RepositoryManager repositoryManager = RepositoryManager.get();
        try {
            RepositoryInfo info = repositoryManager.get(repository);
            File locFile = new File(info.getLocation());
            return locFile.toURI();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }


}
