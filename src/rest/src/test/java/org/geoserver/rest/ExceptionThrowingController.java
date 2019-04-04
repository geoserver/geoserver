/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/exception")
public class ExceptionThrowingController {

    @GetMapping
    public void handleGet(
            @RequestParam(name = "message", required = false) String message,
            @RequestParam(name = "code", required = false) Integer code) {

        throw new RestException(
                message != null ? message : "Unknown error",
                code != null ? HttpStatus.valueOf(code) : HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
