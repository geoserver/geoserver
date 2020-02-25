/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

/**
 * Handles an exception thrown by a service.
 *
 * <p>A service exception handler must declare the services in which it is capable of handling
 * exceptions for, see {@link #getServices()}.
 *
 * <p>Instances must be declared in a spring context as follows:
 *
 * <pre>
 *         <code>
 *  &lt;bean id="myServiceExcepionHandler" class="com.xyz.MyServiceExceptionHandler"&gt;
 *     &lt;constructor-arg ref="myService"/&gt;
 *  &lt;/bean&gt;
 * </code>
 * </pre>
 *
 * <p>Where <code>myService</code> is the id of another bean somewhere in the context.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class ServiceExceptionHandler {

    protected static final String DEFAULT_XML_MIME_TYPE = "application/xml";

    /** Logger */
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.ows");

    /** The services this handler handles exceptions for. */
    List /*<Service>*/ services;

    /**
     * Constructs the handler with the list of {@link Service}'s that it handles exceptions for.
     *
     * @param services A list of {@link Service}.
     */
    public ServiceExceptionHandler(List services) {
        this.services = services;
    }

    /**
     * Constructs the handler for a single {@link Service} that it handles exceptions for.
     *
     * @param service The service to handle exceptions for.
     */
    public ServiceExceptionHandler(Service service) {
        this.services = Collections.singletonList(service);
    }

    /** @return The services this handler handles exceptions for. */
    public List getServices() {
        return services;
    }

    /**
     * Handles the service exception.
     *
     * @param exception The service exception.
     * @param request The informations collected by the dispatcher about the request
     */
    public abstract void handleServiceException(ServiceException exception, Request request);
}
