/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.rest;

import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.geofence.cache.CacheManager;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** @author Emanuele Tajariol (etj at geo-solutions.it) */
@RestController
@ControllerAdvice
@RequestMapping(
        path = {
            RestBaseController.ROOT_PATH + "/geofence/ruleCache",
            RestBaseController.ROOT_PATH + "/ruleCache",
            RestBaseController.ROOT_PATH + "/geofence/cache",
        }) // legacy entrypoint
public class CacheController extends AbstractCatalogController {

    static final Logger LOGGER = Logging.getLogger(CacheController.class);

    @Autowired
    private CacheManager cacheManager;

    public CacheController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
            path = "/info",
            produces = {MediaType.TEXT_PLAIN_VALUE})
    public String getCacheInfo() {

        StringBuilder sb = new StringBuilder();

        appendStats(sb, "RuleStats", cacheManager.getRuleCache());
        appendStats(sb, "AdminAuthStats", cacheManager.getAuthCache());
        appendStats(sb, "UserStats", cacheManager.getUserCache());
        appendStats(sb, "ContStats", cacheManager.getContainerCache());

        return sb.toString();
    }

    private void appendStats(StringBuilder sb, String label, LoadingCache cache) {
        CacheStats stats = cache.stats();
        sb.append(label)
                .append("[ size:")
                .append(cache.size())
                .append("/")
                .append(cacheManager.getCacheInitParams().getSize())
                .append(" hitCount:")
                .append(stats.hitCount())
                .append(" missCount:")
                .append(stats.missCount())
                .append(" loadSuccessCount:")
                .append(stats.loadSuccessCount())
                .append(" loadExceptionCount:")
                .append(stats.loadExceptionCount())
                .append(" totalLoadTime:")
                .append(stats.totalLoadTime())
                .append(" evictionCount:")
                .append(stats.evictionCount())
                .append("] \n");
    }

    @PutMapping(produces = {MediaType.TEXT_PLAIN_VALUE})
    @RequestMapping(path = "/invalidate")
    public String invalidateCache() {
        LOGGER.log(Level.WARNING, "INVALIDATING CACHE");
        cacheManager.invalidateAll();
        return "OK";
    }
}
