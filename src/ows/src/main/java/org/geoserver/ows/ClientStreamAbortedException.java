/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.Serial;
import java.io.UncheckedIOException;

/**
 * An IOException that means a {@link ServiceStrategy#getDestination(jakarta.servlet.http.HttpServletResponse)
 * ServiceStrategy's destination} IO operation has been abruptly interrupted while writing a response.
 *
 * <p>This exception serves as an indicator to the dispatching system that there's no need to report the exception back
 * to the client.
 *
 * @author Gabriel Roldan (TOPP)
 * @since 1.6.x
 */
public final class ClientStreamAbortedException extends IOException {

    @Serial
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

    /**
     * @throws UncheckedIOException if the causal chain of {@code exception} contains a
     *     {@code ClientStreamAbortedException}, with it as the cause, letting the {@link Dispatcher} abort the current
     *     request gracefully.
     */
    public static void rethrowUncheked(Exception exception) {
        Throwables.getCausalChain(exception).stream()
                .filter(ClientStreamAbortedException.class::isInstance)
                .map(ClientStreamAbortedException.class::cast)
                .findFirst()
                .ifPresent(cause -> {
                    throw new java.io.UncheckedIOException(cause);
                });
    }
}
