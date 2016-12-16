/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.geoserver.rest.util.RESTUtils;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;


/**
 * @author Morgan Thompson - Boundless
 *
 */
public class AboutStatusFinder extends Finder {

    protected AboutStatusFinder() {
        super();
    }

    public Resource findTarget(Request request, Response response) {
        String module = RESTUtils.getAttribute(request, "module");
        return new AboutStatus(getContext(), request, response, module);
    }
}
