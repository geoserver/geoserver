package org.geoserver.restng.catalog;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.restng.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Base controller for catalog info requests
 */
public class CatalogController extends RestController {

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
