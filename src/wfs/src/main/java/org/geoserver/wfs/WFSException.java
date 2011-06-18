/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.platform.ServiceException;


/**
 * WFS application specific exception.
 */
public class WFSException extends ServiceException {
    public WFSException(String message) {
        super(message);
    }

    public WFSException(String message, String code, String locator) {
        super(message, code, locator);
    }

    public WFSException(String message, String code) {
        super(message, code);
    }

    public WFSException(String message, Throwable cause, String code, String locator) {
        super(message, cause, code, locator);
    }

    public WFSException(String message, Throwable cause, String code) {
        super(message, cause, code);
    }

    public WFSException(String message, Throwable cause) {
        super(message, cause);
    }

    public WFSException(Throwable cause, String code, String locator) {
        super(cause, code, locator);
    }

    public WFSException(Throwable cause, String code) {
        super(cause, code);
    }

    public WFSException(Throwable cause) {
        super(cause);
    }
}
