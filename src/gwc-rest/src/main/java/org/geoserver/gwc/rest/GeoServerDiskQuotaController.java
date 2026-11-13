/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.gwc.GWC;
import org.geotools.util.logging.Logging;
import org.geowebcache.diskquota.rest.controller.DiskQuotaController;
import org.geowebcache.util.ApplicationContextProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Applies a disk quota configuration change made through REST to the running quota store and monitor, as the GeoServer
 * user interface does.
 */
@RestController
@RequestMapping(path = "${gwc.context.suffix:}/rest")
public class GeoServerDiskQuotaController extends DiskQuotaController {

    static final Logger LOGGER = Logging.getLogger(GeoServerDiskQuotaController.class);

    public GeoServerDiskQuotaController(ApplicationContextProvider appCtx) {
        super(appCtx);
    }

    @Override
    @RequestMapping(value = "/diskquota", method = RequestMethod.PUT)
    public synchronized ResponseEntity<?> doPut(HttpServletRequest request) {
        ResponseEntity<?> response = super.doPut(request);
        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                GWC.get().reloadDiskQuotaStore();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to apply the saved disk quota configuration", e);
                return new ResponseEntity<>(
                        "Disk quota configuration saved, but applying it failed: " + e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return response;
    }
}
