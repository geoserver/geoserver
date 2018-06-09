/*
 *  Copyright (C) 2014 GeoSolutions S.A.S.
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

package org.geoserver.geofence.config;

import java.io.IOException;
import java.util.logging.Logger;
import org.geoserver.geofence.cache.CacheConfiguration;
import org.geoserver.geofence.cache.CachedRuleReader;
import org.geotools.util.logging.Logging;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceConfigurationController {

    private static final Logger LOGGER = Logging.getLogger(GeoFenceConfigurationController.class);

    private GeoFenceConfigurationManager configurationManager;

    private CachedRuleReader cachedRuleReader;

    public void setConfigurationManager(GeoFenceConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public void setCachedRuleReader(CachedRuleReader cachedRuleReader) {
        this.cachedRuleReader = cachedRuleReader;
    }

    /**
     * Updates the configuration.
     *
     * <p>Sets the config into the manager and forces the classes needing to refresh to do so. Then
     * stores the config to disk.
     */
    public void storeConfiguration(GeoFenceConfiguration gfConfig, CacheConfiguration cacheConfig)
            throws IOException {

        // set the probe configuration. the access manager performs a getCOnfiguration wheneven
        // needed
        configurationManager.setConfiguration(gfConfig);

        // set config and recreates the cache
        configurationManager.setCacheConfiguration(cacheConfig);
        cachedRuleReader.init();

        // write the config to disk
        configurationManager.storeConfiguration();
    }
}
