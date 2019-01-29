/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geowebcache.conveyor.Conveyor;
import org.geowebcache.service.HttpErrorCodeException;

public class ResourceConveyor extends Conveyor {

    private final ResourceFactory.Resource resource;

    protected ResourceConveyor(
            HttpServletRequest request,
            HttpServletResponse response,
            ResourceFactory.Resource resource) {
        super(null, null, request, response);
        this.resource = resource;
        this.reqHandler = Conveyor.RequestHandler.SERVICE;
    }

    public void execute() throws IOException {
        switch (servletReq.getMethod().toUpperCase()) {
            case "GET":
                resource.get(servletResp);
                break;
            case "PUT":
                resource.put(servletResp);
                break;
            case "DELETE":
                resource.delete(servletResp);
                break;
            default:
                throw new HttpErrorCodeException(
                        BAD_REQUEST.value(), "Unsupported HTTP method: " + servletReq.getMethod());
        }
    }
}
