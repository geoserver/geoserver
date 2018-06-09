/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver;

import org.geoserver.ows.ServiceExceptionHandler;

/**
 * DOCUMENT ME!
 *
 * @author Gabriel Rold�n
 * @version $revision$
 * @deprecated implement {@link ServiceExceptionHandler} instead
 */
public interface ExceptionHandler {
    /**
     * DOCUMENT ME!
     *
     * @param message DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public ServiceException newServiceException(String message);

    /**
     * DOCUMENT ME!
     *
     * @param message DOCUMENT ME!
     * @param locator DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public ServiceException newServiceException(String message, String locator);

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public ServiceException newServiceException(Throwable e);

    /**
     * DOCUMENT ME!
     *
     * @param e DOCUMENT ME!
     * @param preMessage DOCUMENT ME!
     * @param locator DOCUMENT ME!
     * @return DOCUMENT ME!
     */
    public ServiceException newServiceException(Throwable e, String preMessage, String locator);

    /*# ServiceException lnkServiceException; */
}
