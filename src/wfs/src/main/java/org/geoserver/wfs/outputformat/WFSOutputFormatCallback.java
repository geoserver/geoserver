/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.outputformat;

import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSInfo;
import org.geotools.util.logging.Logging;

/**
 * A callback that will filter the {@link WFSGetFeatureOutputFormat} based on {@link
 * WFSInfo#getGetFeatureOutputTypes()}
 */
public class WFSOutputFormatCallback extends AbstractDispatcherCallback {
    private static final Logger LOGGER = Logging.getLogger(WFSOutputFormatCallback.class);
    private final GeoServer geoserver;
    private static final String WFS = "wfs";
    private static final String GET_FEATURE = "GetFeature";
    private static final String GET_FEATURE_WITH_LOCK = "GetFeatureWithLock";
    public static final String INVALID_PARAMETER_VALUE = "InvalidParameterValue";

    public WFSOutputFormatCallback(GeoServer geoserver) {
        this.geoserver = geoserver;
    }

    /**
     * Filters out Requests whose Output Format are not explicitly enabled if Get Feature Output
     * Type Checking is enabled
     *
     * @param request The request.
     * @param operation The operation for the request.
     * @return operation if Requested Output Format is allowed
     */
    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        Service service = operation.getService();
        if (service == null
                || !WFS.equalsIgnoreCase(service.getId())
                || (!request.getRequest().equalsIgnoreCase(GET_FEATURE))
                        && (!request.getRequest().equalsIgnoreCase(GET_FEATURE_WITH_LOCK))) {
            // not a WFS service or not getFeature or not getFeatureWithLock so we are not
            // interested in it
            return operation;
        }
        WFSInfo wfs = geoserver.getService(WFSInfo.class);
        if (!wfs.isGetFeatureOutputTypeCheckingEnabled()) {
            // output type checking is disabled, so we are not interested in it
            return operation;
        }
        // filter the output formats
        String outputFormat = request.getOutputFormat();
        if (outputFormat != null
                && wfs.getGetFeatureOutputTypes() != null
                && !wfs.getGetFeatureOutputTypes().contains(outputFormat)) {
            LOGGER.fine(
                    "Output Format "
                            + outputFormat
                            + " is not enabled for GetFeature due to Global WFS settings");
            throw InvalidParameterException(outputFormat);
        }
        return operation;
    }
    /**
     * Builds a ServiceException for an unallowed output format
     *
     * @param outputFormat the output format requested
     * @return the ServiceException
     */
    public ServiceException InvalidParameterException(String outputFormat) {
        ServiceException e =
                new ServiceException(
                        "Invalid Output Format Parameter " + outputFormat, INVALID_PARAMETER_VALUE);
        return e;
    }
}
