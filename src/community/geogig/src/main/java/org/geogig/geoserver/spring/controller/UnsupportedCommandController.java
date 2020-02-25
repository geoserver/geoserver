/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.controller;

import static org.locationtech.geogig.rest.repository.RepositoryProvider.BASE_REPOSITORY_ROUTE;
import static org.locationtech.geogig.rest.repository.RepositoryProvider.GEOGIG_ROUTE_PREFIX;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import org.locationtech.geogig.spring.controller.AbstractController;
import org.locationtech.geogig.web.api.CommandSpecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for disabling specific geogig endpoints. */
@RestController
@RequestMapping(
    path = GEOGIG_ROUTE_PREFIX + "/" + BASE_REPOSITORY_ROUTE + "/{repoName}/",
    produces = {APPLICATION_XML_VALUE, APPLICATION_JSON_VALUE}
)
public class UnsupportedCommandController extends AbstractController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UnsupportedCommandController.class);

    /** List any unsupported commands in this RequestMapping annotation. */
    @RequestMapping(value = {"/rename"})
    public void rename() {
        throw new CommandSpecException(
                "This command is unsupported by the GeoGig plugin.", HttpStatus.BAD_REQUEST);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
