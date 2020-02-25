/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import org.geoserver.platform.ServiceException;

/**
 * Extension of ServiceException for returning a WebMap with the exception. Used with WMS option
 * EXCEPTIONS=PARTIALMAP to return a partial image if there is an exception (such as a timeout)
 * thrown when rendering a WMS request.
 */
public class WMSPartialMapException extends ServiceException {
    WebMap map;
    /**
     * Constructs the exception from a message.
     *
     * @param message The message describing the exception.
     * @param map WebMap associated with the WMS request that threw the exception
     */
    public WMSPartialMapException(String message, WebMap map) {
        super(message);
        this.map = map;
    }

    /**
     * Constructs the exception from a message and causing exception.
     *
     * @param message The message describing the exception.
     * @param cause The case of the exception.
     * @param map WebMap associated with the WMS request that threw the exception
     */
    public WMSPartialMapException(String message, Throwable cause, WebMap map) {
        super(message, cause);
        this.map = map;
    }

    /**
     * Constructs the exception from a message, causing exception, and code.
     *
     * @param message The message describing the exception.
     * @param cause The case of the exception.
     * @param code The application specific exception code for the exception.
     * @param map WebMap associated with the WMS request that threw the exception
     */
    public WMSPartialMapException(String message, Throwable cause, String code, WebMap map) {
        super(message, cause, code);
        this.map = map;
    }

    /**
     * Constructs the exception from a message, causing exception, code, and locator.
     *
     * @param message The message describing the exception.
     * @param cause The case of the exception.
     * @param code The application specific exception code for the exception.
     * @param locator The application specific locator for the exception.
     * @param map WebMap associated with the WMS request that threw the exception
     */
    public WMSPartialMapException(
            String message, Throwable cause, String code, String locator, WebMap map) {
        super(message, cause, code, locator);
        this.map = map;
    }

    /**
     * Constructs the exception from a message, and code.
     *
     * @param message The message describing the exception.
     * @param code The application specific exception code for the exception.
     * @param map WebMap associated with the WMS request that threw the exception
     */
    public WMSPartialMapException(String message, String code, WebMap map) {
        super(message, code);
        this.map = map;
    }

    /**
     * Constructs the exception from a message,code, and locator.
     *
     * @param message The message describing the exception.
     * @param code The application specific exception code for the exception.
     * @param locator The application specific locator for the exception.
     * @param map WebMap associated with the WMS request that threw the exception
     */
    public WMSPartialMapException(String message, String code, String locator, WebMap map) {
        super(message, code, locator);
        this.map = map;
    }

    /**
     * Constructs the exception from a causing exception.
     *
     * @param cause The case of the exception.
     * @param map WebMap associated with the WMS request that threw the exception
     */
    public WMSPartialMapException(Throwable cause, WebMap map) {
        super(cause);
        this.map = map;
    }

    /**
     * Constructs the exception from causing exception, and code.
     *
     * @param cause The case of the exception.
     * @param code The application specific exception code for the exception.
     * @param map WebMap associated with the WMS request that threw the exception
     */
    public WMSPartialMapException(Throwable cause, String code, WebMap map) {
        super(cause, code);
        this.map = map;
    }

    /**
     * Constructs the exception from a causing exception, code, and locator.
     *
     * @param cause The case of the exception.
     * @param code The application specific exception code for the exception.
     * @param locator The application specific locator for the exception.
     * @param map WebMap associated with the WMS request that threw the exception
     */
    public WMSPartialMapException(Throwable cause, String code, String locator, WebMap map) {
        super(cause, code, locator);
        this.map = map;
    }

    public WebMap getMap() {
        return map;
    }
}
