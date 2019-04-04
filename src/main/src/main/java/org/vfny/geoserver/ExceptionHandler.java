/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver;

import org.geoserver.ows.ServiceExceptionHandler;

/**
 * @author Gabriel Rold�n
 * @version $revision$
 * @deprecated implement {@link ServiceExceptionHandler} instead
 */
public interface ExceptionHandler {
    /** */
    public ServiceException newServiceException(String message);

    /** */
    public ServiceException newServiceException(String message, String locator);

    /** */
    public ServiceException newServiceException(Throwable e);

    /** */
    public ServiceException newServiceException(Throwable e, String preMessage, String locator);

    /*# ServiceException lnkServiceException; */
}
