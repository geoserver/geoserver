/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.restlet.data.Form;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class ExceptionThrowingResource extends Resource {

    @Override
    public void handleGet() {
        Form f = getRequest().getResourceRef().getQueryAsForm();
        String message= f.getFirstValue("message");
        String code = f.getFirstValue("code");
        
        throw new RestletException( message != null ? message : "Unknown error",  
                code != null ? new Status( Integer.parseInt(code)) : Status.SERVER_ERROR_INTERNAL );
    }
}
