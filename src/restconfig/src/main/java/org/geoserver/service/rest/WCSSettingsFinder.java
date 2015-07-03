/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.service.rest;

import org.geoserver.config.GeoServer;
import org.geoserver.rest.AbstractGeoServerFinder;
import org.geoserver.wcs.WCSInfo;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class WCSSettingsFinder extends AbstractGeoServerFinder {

    protected WCSSettingsFinder(GeoServer geoServer) {
        super(geoServer);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        return new WCSSettingsResource(getContext(), request, response, WCSInfo.class, geoServer);
    }

}
