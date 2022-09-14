package org.geoserver.gwc.controller;

import org.geoserver.catalog.Catalog;

/** Handler for mapping workspace-based demo requests to non-workspace-based requests. */
public class GwcDemoUrlHandlerMapping extends GwcWmtsRestUrlHandlerMapping {

    public GwcDemoUrlHandlerMapping(Catalog catalog) {
        super(catalog);
        handlerMappingString = "/gwc/demo";
    }
}
