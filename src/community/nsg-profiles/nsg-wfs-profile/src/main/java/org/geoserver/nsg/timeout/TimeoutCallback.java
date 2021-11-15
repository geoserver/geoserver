/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.timeout;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.opengis.wfs20.BaseRequestType;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.util.Converters;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;

/** Callback implementing NSG timeout extension */
public class TimeoutCallback extends AbstractDispatcherCallback {

    static final Logger LOGGER = Logging.getLogger(TimeoutCallback.class);

    /** The key storing the NSG request timeout in the {@link WFSInfo#getMetadata()} map */
    public static final String TIMEOUT_CONFIG_KEY = "org.geoserver.nsg.timeout";

    /** The default timeout according to specification (5 minutes) */
    public static final int TIMEOUT_CONFIG_DEFAULT = 300;

    static final String TIMEOUT_REQUEST_ATTRIBUTE = "timeout";

    static final Version V_20 = new Version("2.0");

    GeoServer gs;

    ThreadLocal<TimeoutVerifier> TIMEOUT_VERIFIER = new ThreadLocal<>();

    public TimeoutCallback(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    public Request init(Request request) {
        return super.init(request);
    }

    @Override
    public Operation operationDispatched(Request request, Operation operation) {
        String version = request.getVersion();
        String method = request.getRequest();
        long timeout = getTimeoutMilliseconds(operation);
        if ("WFS".equalsIgnoreCase(request.getService())
                && (version == null || V_20.compareTo(new Version(version)) <= 0)
                && method != null
                && (method.equalsIgnoreCase("GetFeature")
                        || method.equalsIgnoreCase("GetFeatureWithLock")
                        || method.equalsIgnoreCase("GetPropertyValue"))
                && timeout > 0
                && operation.getParameters().length > 0
                && operation.getParameters()[0] instanceof BaseRequestType) {

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Starting to track NSG timeout on this request");
            }

            // start tracking time
            TimeoutVerifier timeoutVerifier =
                    new TimeoutVerifier((BaseRequestType) operation.getParameters()[0], timeout);
            // need to wrap the http response and its output stream
            request.setHttpResponse(
                    new TimeoutCancellingResponse(request.getHttpResponse(), timeoutVerifier));
            // set in the thread local for later use
            TIMEOUT_VERIFIER.set(timeoutVerifier);
        }

        return operation;
    }

    @Override
    public void finished(Request request) {
        TIMEOUT_VERIFIER.remove();
    }

    @Override
    public Object operationExecuted(Request request, Operation operation, Object result) {
        TimeoutVerifier timeoutVerifier = TIMEOUT_VERIFIER.get();
        if (timeoutVerifier != null) {
            // check before encode
            timeoutVerifier.checkTimeout();

            // wrap if needed
            if (result instanceof FeatureCollectionResponse) {
                FeatureCollectionResponse featureCollectionResponse =
                        (FeatureCollectionResponse) result;
                List<FeatureCollection> collections = featureCollectionResponse.getFeatures();
                List<FeatureCollection> wrappers =
                        collections
                                .stream()
                                .map(fc -> TimeoutFeatureCollection.wrap(timeoutVerifier, fc))
                                .collect(Collectors.toList());

                featureCollectionResponse.setFeatures(wrappers);
            }
        }

        return result;
    }

    private long getTimeoutMilliseconds(Operation operation) {
        // check if there is a timeout parameter
        Object[] parameters = operation.getParameters();
        if (parameters != null
                && parameters.length > 0
                && parameters[0] instanceof BaseRequestType) {
            BaseRequestType request = (BaseRequestType) parameters[0];
            Object timeout = request.getExtendedProperties().get(TIMEOUT_REQUEST_ATTRIBUTE);
            if (timeout != null) {
                Long converted = Converters.convert(timeout, Long.class);
                if (converted != null && converted > 0) {
                    return converted * 1000l;
                } else {
                    throw new WFSException(request, "Invalid timeout value: " + timeout);
                }
            }
        }

        // use the configured default
        WFSInfo wfs = gs.getService(WFSInfo.class);
        Integer timeoutSeconds = wfs.getMetadata().get(TIMEOUT_CONFIG_KEY, Integer.class);
        return Optional.ofNullable(timeoutSeconds).orElse(TIMEOUT_CONFIG_DEFAULT) * 1000L;
    }
}
