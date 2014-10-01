/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs;

import org.geoserver.platform.ServiceException;


/**
 * WFS application specific exception.
 */
public class TJSException extends ServiceException {
    public TJSException(String message) {
        super(message);
    }

    public TJSException(String message, String code, String locator) {
        super(message, code, locator);
    }

    public TJSException(String message, String code) {
        super(message, code);
    }

    public TJSException(String message, Throwable cause, String code, String locator) {
        super(message, cause, code, locator);
    }

    public TJSException(String message, Throwable cause, String code) {
        super(message, cause, code);
    }

    public TJSException(String message, Throwable cause) {
        super(message, cause);
    }

    public TJSException(Throwable cause, String code, String locator) {
        super(cause, code, locator);
    }

    public TJSException(Throwable cause, String code) {
        super(cause, code);
    }

    public TJSException(Throwable cause) {
        super(cause);
    }
}
