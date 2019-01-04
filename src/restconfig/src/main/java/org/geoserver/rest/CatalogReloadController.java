/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.geoserver.config.GeoServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/** Catalog reload controller */
@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class CatalogReloadController extends AbstractGeoServerController {

    @Autowired
    public CatalogReloadController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @RequestMapping(
        value = "/reload",
        method = {RequestMethod.POST, RequestMethod.PUT}
    )
    public void reload() throws Exception {
        geoServer.reload();
    }

    @RequestMapping(
        value = "/reset",
        method = {RequestMethod.POST, RequestMethod.PUT}
    )
    public void reset() {
        geoServer.reset();
    }
}
