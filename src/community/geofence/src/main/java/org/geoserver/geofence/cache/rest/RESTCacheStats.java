/*
 *  Copyright (C) 2007 - 2012 GeoSolutions S.A.S.
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
package org.geoserver.geofence.cache.rest;

import com.google.common.base.Objects;
import com.google.common.cache.CacheStats;

import org.geoserver.geofence.cache.CachedRuleReader;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class RESTCacheStats extends Resource {

    private final CachedRuleReader crr;

    RESTCacheStats(Context context, Request request, Response response, CachedRuleReader cachedRuleReader) {
        super(context, request, response);
        this.crr = cachedRuleReader;
    }

    @Override
    public void handleGet() {
        //        Representation representation = new StringRepresentation(stats.toString());
        //        getResponse().setEntity(representation);
        CacheStats stats = crr.getStats();

        StringBuilder sb = new StringBuilder()
                .append("RuleStats[")
                .append(" size:").append(crr.getCacheSize())
                .append("/").append(crr.getCacheInitParams().getSize())
                .append(" hitCount:").append(stats.hitCount())
                .append(" missCount:").append(stats.missCount())
                .append(" loadSuccessCount:").append(stats.loadSuccessCount())
                .append(" loadExceptionCount:").append(stats.loadExceptionCount())
                .append(" totalLoadTime:").append(stats.totalLoadTime())
                .append(" evictionCount:").append(stats.evictionCount())
                .append("] \n");

        stats = crr.getUserStats();
        sb.append("UserStats[")
                .append(" size:").append(crr.getUserCacheSize())
                .append("/").append(crr.getCacheInitParams().getSize())
                .append(" hitCount:").append(stats.hitCount())
                .append(" missCount:").append(stats.missCount())
                .append(" loadSuccessCount:").append(stats.loadSuccessCount())
                .append(" loadExceptionCount:").append(stats.loadExceptionCount())
                .append(" totalLoadTime:").append(stats.totalLoadTime())
                .append(" evictionCount:").append(stats.evictionCount())
                .append("] \n");

       getResponse().setEntity(new StringRepresentation(sb));
    }
}
