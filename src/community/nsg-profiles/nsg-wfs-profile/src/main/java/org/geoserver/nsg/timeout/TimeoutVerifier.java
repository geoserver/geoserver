/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.timeout;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.wfs20.BaseRequestType;
import org.geoserver.wfs.WFSException;
import org.geotools.util.logging.Logging;

/** Simple timeout checker */
class TimeoutVerifier {

    static final Logger LOGGER = Logging.getLogger(TimeoutVerifier.class);

    static final String TIMEOUT_EXCEPTION_CODE = "OperationProcessingTimeout";

    long timeoutTime;
    BaseRequestType request;
    boolean cancelled = false;
    boolean thrown = false;

    TimeoutVerifier(BaseRequestType request, long timeout) {
        this.request = request;
        this.timeoutTime = System.currentTimeMillis() + timeout;
    }

    void checkTimeout() {
        if (!cancelled && System.currentTimeMillis() > timeoutTime) {
            this.thrown = true;
            throw new WFSException(request, "Timeout exceeded", TIMEOUT_EXCEPTION_CODE);
        }
    }

    public void cancel() {
        if (!this.cancelled) {
            if (!this.thrown) {
                LOGGER.log(
                        Level.FINE,
                        "Timeout cancelled (presumably as GeoServer started to write out the response)");
            }
            this.cancelled = true;
        }
    }
}
