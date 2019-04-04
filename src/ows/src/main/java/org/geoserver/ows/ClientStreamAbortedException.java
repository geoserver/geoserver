/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.IOException;

/**
 * An IOException that means a {@link
 * ServiceStrategy#getDestination(javax.servlet.http.HttpServletResponse) ServiceStrategy's
 * destination} IO operation has been abruptly interrupted while writing a response.
 *
 * <p>This exception serves as an indicator to the dispatching system that there's no need to report
 * the exception back to the client.
 *
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 * @since 1.6.x
 */
public final class ClientStreamAbortedException extends IOException {

    private static final long serialVersionUID = -812677957232110980L;

    public ClientStreamAbortedException() {
        super();
    }

    public ClientStreamAbortedException(String message) {
        super(message);
    }

    public ClientStreamAbortedException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    public ClientStreamAbortedException(Throwable cause) {
        super();
        initCause(cause);
    }
}
