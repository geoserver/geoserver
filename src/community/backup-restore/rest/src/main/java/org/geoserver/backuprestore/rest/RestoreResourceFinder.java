/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import org.geoserver.rest.BeanResourceFinder;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * REST resource finder for 
 * 
 * <pre>/br/restore[/&lt;restoreId&gt;][.zip]</pre>
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class RestoreResourceFinder extends BeanResourceFinder {

    /**
     * @param beanName
     */
    public RestoreResourceFinder(String beanName) {
        super(beanName);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        RestoreResource restoreResource = (RestoreResource) super.findTarget(request, response);
        
        final String format = RESTUtils.getAttribute(request, "format");
        
        //if (request.getResourceRef().getLastSegment().endsWith(".zip")) {
        if ("zip".equals(format)) {
            final RestoreStreamResource restoreStreamResource = new RestoreStreamResource(restoreResource.getBackupFacade());
            restoreStreamResource.setRequest(request);
            restoreStreamResource.setResponse(response);
            return restoreStreamResource;
        } else {
            return restoreResource;
        }
    }

    
}
