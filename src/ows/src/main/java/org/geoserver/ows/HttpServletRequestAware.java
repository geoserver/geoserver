/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import javax.servlet.http.HttpServletRequest;

/**
 * An interface for request readers to implement to flag to the dispatcher that they wish to be
 * given an instance of the current http request.
 *
 * <p>Note: This interface is added to allow existing services to adapt to the new ows dispatch
 * framework.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @deprecated Client code should not implement this interface
 */
public interface HttpServletRequestAware {
    void setHttpRequest(HttpServletRequest request);
}
