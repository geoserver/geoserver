/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.restupload;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.rest.AbstractCatalogFinder;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * {@link Finder} implementation returning a {@link ResumableUploadCatalogResource}.
 *
 * @author Nicola Lagomarsini
 */
public class ResumableUploadCatalogFinder extends AbstractCatalogFinder {
    /** Manager for the Resumable REST upload */
    private ResumableUploadResourceManager resumableUploadResourceManager;

    protected ResumableUploadCatalogFinder(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        return new ResumableUploadCatalogResource(
                getContext(), request, response, catalog, resumableUploadResourceManager);
    }

    public void setResumableUploadResourceManager(
            ResumableUploadResourceManager resumableUploadResourceManager) {
        this.resumableUploadResourceManager = resumableUploadResourceManager;
    }
}
