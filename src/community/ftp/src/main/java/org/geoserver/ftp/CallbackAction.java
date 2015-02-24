/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ftp;

/**
 * Defines the possible actions to be taken by any of the {@link FTPCallback} methods, indicating
 * whether processing of the given request by other callbacks and the FTP server should continue
 * normally, be ignored, or the client connection should be shut down immediately.
 * 
 * @author groldan
 * @see FTPCallback
 */
public enum CallbackAction {
    /**
     * This return value indicates that the next ftplet method will be called. If no other ftplet is
     * available, the ftpserver will process the request.
     */
    CONTINUE,

    /**
     * It indicates that the ftpserver will skip everything. No further processing will be done for
     * this request, and no other {@link FTPCallback callbacks} will be notified of the current
     * event.
     */
    SKIP,

    /**
     * It indicates that the server will skip and disconnect the client. No other request from the
     * same client will be served.
     */
    DISCONNECT;
}
