/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.rest.RestBaseController;

import java.util.Arrays;
import java.util.List;

/**
 * Base controller for catalog info requests
 */
public abstract class CatalogController extends RestBaseController {
    
    /**
     * Not an official MIME type, but GeoServer used to support it
     */
    public static final String TEXT_JSON = "text/json";
    /**
     * Not an official MIME type, but GeoServer used to support it
     */
    public static final String APPLICATION_ZIP = "application/zip";

    protected final Catalog catalog;
    protected final GeoServerDataDirectory dataDir;

    protected final List<String> validImageFileExtensions;

    public CatalogController(Catalog catalog) {
        super();
        this.catalog = catalog;
        this.dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        this.validImageFileExtensions = Arrays.asList("svg", "png", "jpg");
    }
}
