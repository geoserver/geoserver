package org.geoserver.gwc.controller;

import org.geoserver.catalog.Catalog;

/**
 * Handler for mapping workspace-based web (i.e. web/openlayers/ol.js) requests to
 * non-workspace-based requests.
 */
public class GwcRestWebUrlHandlerMapping extends GwcWmtsRestUrlHandlerMapping {

    public GwcRestWebUrlHandlerMapping(Catalog catalog) {
        super(catalog);
        handlerMappingString = "/gwc/rest/web";
    }
}
