/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.controller;

import org.geoserver.catalog.Catalog;

/**
 * Handler for mapping workspace-based web (i.e. web/openlayers/ol.js) requests to
 * non-workspace-based requests.
 */
public class GwcRestWebUrlHandlerMapping extends GwcWmtsRestUrlHandlerMapping {

    public GwcRestWebUrlHandlerMapping(Catalog catalog) {
        super(catalog);
        GWC_URL_PATTERN = "/gwc/rest/web";
    }
}
