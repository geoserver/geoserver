/* (c) 2014-2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import org.geoserver.wps.executor.MaxExecutionTimeListener;
import org.geotools.data.util.DelegateProgressListener;
import org.opengis.util.ProgressListener;

/**
 * Exception used to "poison" inputs and listener methods to force processes to exit when a dismiss
 * request was submitted
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ProcessDismissedException extends RuntimeException {

    private static final long serialVersionUID = -4266240008696107774L;

    public ProcessDismissedException() {
        this("The process execution has been dismissed");
    }

    public ProcessDismissedException(ProgressListener listener) {
        this(getMessageFromListener(listener));
    }

    private static String getMessageFromListener(ProgressListener listener) {
        // see if we went beyond the maximum time allowed
        while (!(listener instanceof MaxExecutionTimeListener)
                && (listener instanceof DelegateProgressListener)) {
            DelegateProgressListener d = (DelegateProgressListener) listener;
            listener = d.getDelegate();
        }

        if (listener instanceof MaxExecutionTimeListener) {
            MaxExecutionTimeListener max = (MaxExecutionTimeListener) listener;
            if (max.isExpired()) {
                return "The process executed got interrupted because it went "
                        + "beyond the configured limits of "
                        + "maxExecutionTime "
                        + (max.getMaxExecutionTime() / 1000)
                        + " seconds, "
                        + "maxTotalTime "
                        + (max.getMaxTotalTime() / 1000)
                        + " seconds";
            }
        }

        return "The process execution has been dismissed";
    }

    public ProcessDismissedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessDismissedException(String message) {
        super(message);
    }

    public ProcessDismissedException(Throwable cause) {
        super(cause);
    }
}
