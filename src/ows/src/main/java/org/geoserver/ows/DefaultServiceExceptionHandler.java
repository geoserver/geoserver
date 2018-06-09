/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.List;

/**
 * A default implementation of {@link ServiceExceptionHandler} which outputs as service exception in
 * a <code>ows:ExceptionReport</code> document.
 *
 * <p>This service exception handler will generate an OWS exception report, see <a
 * href="http://schemas.opengis.net/ows/1.0.0/owsExceptionReport.xsd">owsExceptionReport.xsd</a> .
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @deprecated use {@link OWS10ServiceExceptionHandler}
 */
public class DefaultServiceExceptionHandler extends OWS10ServiceExceptionHandler {

    public DefaultServiceExceptionHandler() {
        super();
    }

    public DefaultServiceExceptionHandler(List services) {
        super(services);
    }
}
