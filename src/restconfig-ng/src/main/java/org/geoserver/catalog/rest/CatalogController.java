package org.geoserver.catalog.rest;

import java.util.Arrays;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.rest.RestController;

/**
 * Base controller for catalog info requests
 */
public class CatalogController extends RestController {
    
    /**
     * Not an official MIME type, but GeoServer used to support it
     */
    public static final String TEXT_JSON = "text/json";

    protected final Catalog catalog;
    protected final GeoServerDataDirectory dataDir;

    protected final List<String> validImageFileExtensions;

    public CatalogController(Catalog catalog) {
        super();
        this.pathPrefix = "templates";
        this.catalog = catalog;
        this.dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
        this.validImageFileExtensions = Arrays.asList("svg", "png", "jpg");
    }
}
