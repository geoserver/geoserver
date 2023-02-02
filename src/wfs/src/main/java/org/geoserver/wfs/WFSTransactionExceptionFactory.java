/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.config.SettingsInfo;

/**
 * Factory for creating WFS_T application specific exception instances, including cause details in
 * exception messages based on the global verbose exceptions setting. This enables eg trigger
 * exception messages to bubble up to end-users in a controlled internal environment, where root
 * cause aids the user in entering correct data and is of no security concern.
 *
 * @author Martin Kalén
 */
public class WFSTransactionExceptionFactory {

    private final SettingsInfo settings;

    /**
     * Create a WFS-T exception factory.
     *
     * @param settings GeoServer settings
     */
    public WFSTransactionExceptionFactory(final SettingsInfo settings) {
        this.settings = settings;
    }

    public WFSTransactionException newWFSTransactionException(
            final String errorMessage, final Throwable cause, final String code) {
        String finalMessage = errorMessage;
        if (settings.isVerboseExceptions()) {
            finalMessage = decorateMessageWithUnderlyingCause(errorMessage, cause);
        }
        return new WFSTransactionException(finalMessage, cause, code);
    }

    public WFSTransactionException newWFSTransactionException(
            final String errorMessage,
            final Throwable cause,
            final String code,
            final String locator,
            final String handle) {
        String finalMessage = errorMessage;
        if (settings.isVerboseExceptions()) {
            finalMessage = decorateMessageWithUnderlyingCause(errorMessage, cause);
        }
        return new WFSTransactionException(finalMessage, cause, code, locator, handle);
    }

    private String decorateMessageWithUnderlyingCause(
            final String errorMessage, final Throwable cause) {
        if (errorMessage == null || cause == null) {
            return errorMessage;
        }
        final StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append(errorMessage);

        Throwable underlyingCause = cause.getCause();
        if (underlyingCause != null) {
            msgBuilder.append(" (");
            while (underlyingCause != null) {
                msgBuilder.append(underlyingCause.getMessage());
                underlyingCause = underlyingCause.getCause();
                if (underlyingCause != null) {
                    msgBuilder.append(", ");
                }
            }
            msgBuilder.append(")");
        }
        return msgBuilder.toString();
    }
}
