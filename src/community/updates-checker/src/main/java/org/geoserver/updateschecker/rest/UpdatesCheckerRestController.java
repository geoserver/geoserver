/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.rest;

import org.geoserver.rest.RestBaseController;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Triggers an on-demand update check over REST, exactly as the settings page's Check Now button does, for scripted or
 * headless use. Only {@code POST} is mapped: the call fetches the feed and refreshes the shared cache, so it must not
 * be reachable by a plain link or an image tag on another site.
 *
 * <p>Access is restricted to administrators the standard GeoServer way: the {@code /rest/**} rules in
 * {@code security/rest.properties}, which by default require the administrator role ({@code ROLE_ADMINISTRATOR}) for
 * every method. This controller adds no separate check of its own.
 */
@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/updates-checker")
public class UpdatesCheckerRestController {

    private final UpdatesCheckerService service;

    public UpdatesCheckerRestController(UpdatesCheckerService service) {
        this.service = service;
    }

    @PostMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public UpdatesCheckerCheckResponse post() {
        return UpdatesCheckerCheckResponse.of(
                service.check(UpdatesCheckerService.Trigger.MANUAL), LocaleContextHolder.getLocale());
    }
}
