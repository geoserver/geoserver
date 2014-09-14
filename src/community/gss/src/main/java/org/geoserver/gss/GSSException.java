/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss;

import org.geoserver.platform.ServiceException;

/**
 * A generic GSS service exception
 */
public class GSSException extends ServiceException {
    private static final long serialVersionUID = 7001211051719019724L;
    
    public enum GSSExceptionCode {
        MissingParameterValue, InvalidParameterValue, NoApplicableCode
    }

    public GSSException(String msg, GSSExceptionCode code, String locator) {
        super(msg, code.name(), locator);
    }

    public GSSException(String msg, GSSExceptionCode code) {
        super(msg, code.name());
    }

    public GSSException(String msg) {
        super(msg);
    }
    
    public GSSException(String msg, Throwable cause) {
        super(msg);
        initCause(cause);
    }

}
