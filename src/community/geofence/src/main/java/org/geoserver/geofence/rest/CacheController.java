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

import com.google.common.cache.CacheStats;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.geofence.cache.CachedRuleReader;
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
        RestBaseController.ROOT_PATH + "/ruleCache"
    }
) // legacy entrypoint
public class CacheController extends AbstractCatalogController {

    static final Logger LOGGER = Logging.getLogger(CacheController.class);

    @Autowired private CachedRuleReader cachedRuleReader;

    public CacheController(Catalog catalog) {
        super(catalog);
    }

    @GetMapping(
        path = "/info",
        produces = {MediaType.TEXT_PLAIN_VALUE}
    )
    public String getCacheInfo() {
        CacheStats stats = cachedRuleReader.getStats();

        StringBuilder sb =
                new StringBuilder()
                        .append("RuleStats[")
                        .append(" size:")
                        .append(cachedRuleReader.getCacheSize())
                        .append("/")
                        .append(cachedRuleReader.getCacheInitParams().getSize())
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

        stats = cachedRuleReader.getAdminAuthStats();
        sb.append("AdminAuthStats[")
                .append(" size:")
                .append(cachedRuleReader.getCacheSize())
                .append("/")
                .append(cachedRuleReader.getCacheInitParams().getSize())
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

        stats = cachedRuleReader.getUserStats();
        sb.append("UserStats[")
                .append(" size:")
                .append(cachedRuleReader.getUserCacheSize())
                .append("/")
                .append(cachedRuleReader.getCacheInitParams().getSize())
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

        return sb.toString();
    }

    @PutMapping(produces = {MediaType.TEXT_PLAIN_VALUE})
    @RequestMapping(path = "/invalidate")
    public String invalidateCache() {
        LOGGER.log(Level.WARNING, "INVALIDATING CACHE");
        cachedRuleReader.invalidateAll();
        return "OK";
    }
}
