/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.rest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.geoserver.updateschecker.UpdatesCheckerStatus;
import org.geoserver.updateschecker.UpdatesCheckerSuppression;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
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

    private static final int MAX_BODY_BYTES = 64 * 1024;

    private final UpdatesCheckerService service;

    public UpdatesCheckerRestController(UpdatesCheckerService service) {
        this.service = service;
    }

    /**
     * @param body optional JSON object whose {@code suppressedCves} array replaces the server's suppressed advisories
     *     for this call only. It is read as a plain stream, not through a message converter: GeoServer's converters
     *     treat any JSON body as one of its own XStream types.
     */
    @PostMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public UpdatesCheckerCheckResponse post(InputStream body) throws IOException {
        UpdatesCheckerSuppression override;
        try {
            override = UpdatesCheckerSuppression.fromRequest(read(body));
        } catch (IllegalArgumentException e) {
            throw new RestException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        UpdatesCheckerStatus status = service.check(UpdatesCheckerService.Trigger.MANUAL, override);
        String suppressedBy = status.getSuppressed().isEmpty() ? null : override != null ? "request" : "environment";
        return UpdatesCheckerCheckResponse.of(status, LocaleContextHolder.getLocale(), suppressedBy);
    }

    private static String read(InputStream body) throws IOException {
        if (body == null) {
            return null;
        }
        byte[] bytes = body.readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) {
            throw new RestException(
                    "The request body is larger than " + MAX_BODY_BYTES + " bytes", HttpStatus.BAD_REQUEST);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
