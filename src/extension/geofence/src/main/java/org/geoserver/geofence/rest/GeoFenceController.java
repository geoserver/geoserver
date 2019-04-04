/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest;

import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** @author Emanuele Tajariol (etj at geo-solutions.it) */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/geofence")
public class GeoFenceController extends AbstractCatalogController {

    static final Logger LOGGER = Logging.getLogger(GeoFenceController.class);

    @Autowired private GeoFenceConfigurationManager configManager;

    public GeoFenceController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        path = "/info",
        produces = {MediaType.TEXT_PLAIN_VALUE}
    )
    public String getInfo() {
        return configManager.getConfiguration().getInstanceName();
    }
}
