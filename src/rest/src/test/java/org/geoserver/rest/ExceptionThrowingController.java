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
public class ExceptionThrowingController {

    @GetMapping
    @RequestMapping(path = RestBaseController.ROOT_PATH + "/exception")
    public void handleException(
            @RequestParam(name = "message", required = false) String message,
            @RequestParam(name = "code", required = false) Integer code) {

        throw new RestException(
                message != null ? message : "Unknown error",
                code != null ? HttpStatus.valueOf(code) : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping
    @RequestMapping(path = RestBaseController.ROOT_PATH + "/error")
    public void handleError() {
        throw new RuntimeException("An internal error occurred");
    }

    @GetMapping
    @RequestMapping(path = RestBaseController.ROOT_PATH + "/notfound")
    public void handleNotFound() {
        throw new ResourceNotFoundException("I'm not there");
    }
}
