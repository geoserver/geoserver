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

package org.geoserver.geofence.rest;

import java.util.logging.Logger;

import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geotools.util.logging.Logging;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.StringRepresentation;

/**
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 *
 */
public class GeoFenceInfo extends Resource {
    static final Logger LOGGER = Logging.getLogger(GeoFenceInfo.class);

    private final GeoFenceConfigurationManager configManager;

    public GeoFenceInfo(GeoFenceConfigurationManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean allowGet() {
        return true;
    }

    @Override
    public boolean allowPut() {
        return false;
    }

    @Override
	public void handleGet() {
		Representation representation = new StringRepresentation(configManager
				.getConfiguration().getInstanceName());
        getResponse().setEntity(representation);
	}
}
