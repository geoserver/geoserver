/*
 *  Copyright (C) 2017 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
