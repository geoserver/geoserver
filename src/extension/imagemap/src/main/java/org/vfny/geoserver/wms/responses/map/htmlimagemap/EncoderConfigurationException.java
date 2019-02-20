/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import org.geoserver.platform.ServiceException;

/** @author Mauro Bartolomeoli */
public class EncoderConfigurationException extends ServiceException {
    /** */
    private static final long serialVersionUID = -791532675720744977L;

    public EncoderConfigurationException(String message) {
        super(message);
    }

    public EncoderConfigurationException(String message, String locator) {
        super(message, locator);
    }

    public EncoderConfigurationException(Throwable e, String message, String locator) {
        super(e, message, locator);
    }
}
