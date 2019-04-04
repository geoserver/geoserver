/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

/**
 * WFS_T application specific exception.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class WFSTransactionException extends WFSException {
    /** handle of the transaction request */
    String handle;

    public WFSTransactionException(String message, String code, String locator, String handle) {
        super(message, code, locator);
        this.handle = handle;
    }

    public WFSTransactionException(String message, String code, String locator) {
        super(message, code, locator);
    }

    public WFSTransactionException(String message, String code) {
        super(message, code);
    }

    public WFSTransactionException(
            String message, Throwable cause, String code, String locator, String handle) {
        super(message, cause, code, locator);
        this.handle = handle;
    }

    public WFSTransactionException(String message, Throwable cause, String code, String locator) {
        super(message, cause, code, locator);
    }

    public WFSTransactionException(String message, Throwable cause, String code) {
        super(message, cause, code);
    }

    public WFSTransactionException(String message, Throwable cause) {
        super(message, cause);
    }

    public WFSTransactionException(String message) {
        super(message);
    }

    public WFSTransactionException(Throwable cause, String code, String locator, String handle) {
        super(cause, code, locator);
        this.handle = handle;
    }

    public WFSTransactionException(Throwable cause, String code, String locator) {
        super(cause, code, locator);
    }

    public WFSTransactionException(Throwable cause, String code) {
        super(cause, code);
    }

    public WFSTransactionException(Throwable cause) {
        super(cause);
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getHandle() {
        return handle;
    }
}
