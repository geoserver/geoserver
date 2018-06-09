/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util.requests.readers;

import java.io.Reader;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.ServiceInfo;
import org.geoserver.platform.ServiceException;
import org.vfny.geoserver.Request;

/**
 * This utility reads in XML requests and returns them as appropriate request objects.
 *
 * @author Rob Hranac, TOPP
 * @author Chris Holmes, TOPP
 * @author Gabriel Rold?n
 * @version $Id$
 */
public abstract class XmlRequestReader {
    /** Class logger */
    protected static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.requests.readers");

    /** The service handling the request * */
    private ServiceInfo serviceConfig;

    /**
     * DOCUMENT ME!
     *
     * @param reader DOCUMENT ME!
     * @return DOCUMENT ME!
     * @throws ServiceException DOCUMENT ME!
     */
    public abstract Request read(Reader reader, HttpServletRequest req) throws ServiceException;

    /**
     * This will create a new XmlRequestReader
     *
     * @param service The config of the service handling the request
     */
    public XmlRequestReader(ServiceInfo service) {
        this.serviceConfig = service;
        ;
    }

    public ServiceInfo getService() {
        return serviceConfig;
    }
}
