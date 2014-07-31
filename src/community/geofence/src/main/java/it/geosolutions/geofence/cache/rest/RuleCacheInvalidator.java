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
package it.geosolutions.geofence.cache.rest;

import it.geosolutions.geofence.cache.CachedRuleReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class RuleCacheInvalidator extends Resource {
    static final Logger LOGGER = Logging.getLogger(RuleCacheInvalidator.class);

    private CachedRuleReader cachedRuleReader;

    public RuleCacheInvalidator(CachedRuleReader cachedRuleReader) {
        this.cachedRuleReader = cachedRuleReader;
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public void handlePut() {
        LOGGER.log(Level.WARNING, "INVALIDATING CACHE");
        cachedRuleReader.invalidateAll();
        Representation representation = new StringRepresentation("OK");
        getResponse().setEntity(representation);
    }



}
