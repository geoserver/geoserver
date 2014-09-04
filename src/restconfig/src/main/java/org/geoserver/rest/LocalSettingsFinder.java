/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class LocalSettingsFinder extends AbstractGeoServerFinder {

    public LocalSettingsFinder(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        return new LocalSettingsResource(getContext(), request, response, SettingsInfo.class,
                geoServer);
    }

}
