/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker.rest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.updateschecker.Status;
import org.geoserver.updateschecker.Suppression;
import org.geoserver.updateschecker.UpdatesCheckerService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runs an update check, as Check Now does. Only {@code POST} is mapped, because the call fetches the feed, so a link or
 * an image on another site cannot trigger it. Access follows the {@code /rest/**} rules of
 * {@code security/rest.properties}.
 */
@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH + "/updates-checker")
public class CheckController {

    private static final int MAX_BODY_BYTES = 64 * 1024;

    private final UpdatesCheckerService service;

    public CheckController(UpdatesCheckerService service) {
        this.service = service;
    }

    /**
     * @param body an optional JSON object whose {@code suppressedCves} array replaces the server's for this call; read
     *     as a stream, because GeoServer's message converters read any JSON body as one of its XStream types
     */
    @PostMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> post(InputStream body) throws IOException {
        Suppression override;
        try {
            override = Suppression.fromRequest(read(body));
        } catch (IllegalArgumentException e) {
            throw new RestException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        Status status = service.check(UpdatesCheckerService.Trigger.MANUAL, override);
        return CheckResponse.of(status, LocaleContextHolder.getLocale(), suppressedBy(status, override));
    }

    private static String suppressedBy(Status status, Suppression override) {
        if (status.getSuppressed().isEmpty()) {
            return null;
        }
        return override == null ? "environment" : "request";
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
