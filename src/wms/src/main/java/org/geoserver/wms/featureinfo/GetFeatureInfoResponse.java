/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import net.opengis.wfs.FeatureCollectionType;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfo;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.springframework.util.Assert;

/**
 * A GetFeatureInfoResponse object is responsible for generating GetFeatureInfo content in the
 * format specified. The way the content is generated is independent of this class, wich will use a
 * delegate object based on the output format requested
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GetFeatureInfoResponse extends Response {

    private final WMS wms;

    private GetFeatureInfoOutputFormat defaultOutputFormat;

    /** Creates a new GetMapResponse object. */
    public GetFeatureInfoResponse(
            final WMS wms, final GetFeatureInfoOutputFormat defaultOutputFormat) {
        super(FeatureCollectionType.class);
        this.wms = wms;
        this.defaultOutputFormat = defaultOutputFormat;
    }

    /** @see org.geoserver.ows.Response#canHandle(org.geoserver.platform.Operation) */
    @Override
    public boolean canHandle(Operation operation) {
        return "GetFeatureInfo".equalsIgnoreCase(operation.getId());
    }

    /**
     * Asks the available GetFeatureInfoOutputFormats for the MIME type of the result that it will
     * generate or is ready to, and returns it
     *
     * @param value a {@link FeatureCollectionType} as returned by {@link GetFeatureInfo}
     * @param operation the {@link GetFeatureInfo} operation that originated the {@code value}
     * @see org.geoserver.ows.Response#getMimeType(java.lang.Object,
     *     org.geoserver.platform.Operation)
     */
    @Override
    public String getMimeType(final Object value, final Operation operation)
            throws ServiceException {
        Assert.notNull(value, "value is null");
        Assert.notNull(operation, "operation is null");
        Assert.isTrue(value instanceof FeatureCollectionType, "unrecognized result type:");

        GetFeatureInfoRequest request =
                (GetFeatureInfoRequest)
                        OwsUtils.parameter(operation.getParameters(), GetFeatureInfoRequest.class);

        Assert.notNull(request, "request");

        GetFeatureInfoOutputFormat outputFormat = getRequestedOutputFormat(request);

        return outputFormat.getContentType();
    }

    /**
     * @param value {@link FeatureCollectionType}
     * @param output where to encode the results to
     * @param operation {@link GetFeatureInfo}
     * @see org.geoserver.ows.Response#write(java.lang.Object, java.io.OutputStream,
     *     org.geoserver.platform.Operation)
     */
    @Override
    public void write(final Object value, final OutputStream output, final Operation operation)
            throws IOException, ServiceException {

        Assert.notNull(value, "value is null");
        Assert.notNull(operation, "operation is null");
        Assert.isTrue(value instanceof FeatureCollectionType, "unrecognized result type:");
        Assert.isTrue(
                operation.getParameters() != null
                        && operation.getParameters().length == 1
                        && operation.getParameters()[0] instanceof GetFeatureInfoRequest,
                "Operation parameters should be a single GetFeatureInfoRequest");

        GetFeatureInfoRequest request = (GetFeatureInfoRequest) operation.getParameters()[0];
        FeatureCollectionType results = (FeatureCollectionType) value;
        GetFeatureInfoOutputFormat outputFormat = getRequestedOutputFormat(request);

        outputFormat.write(results, request, output);
    }

    /**
     * @throws ServiceException if no {@link GetFeatureInfoOutputFormat} is configured for the
     *     output format specified in <code>request</code>
     */
    private GetFeatureInfoOutputFormat getRequestedOutputFormat(GetFeatureInfoRequest request)
            throws ServiceException {

        String requestFormat = request.getInfoFormat();

        GetFeatureInfoOutputFormat format = wms.getFeatureInfoOutputFormat(requestFormat);
        if (format == null) {
            format = defaultOutputFormat;
        }

        if (wms.isAllowedGetFeatureInfoFormat(format) == false) {
            throw wms.unallowedGetFeatureInfoFormatException(requestFormat);
        }

        return format;
    }

    @Override
    public String getCharset(Operation operation) {
        Assert.notNull(operation, "operation is null");
        GetFeatureInfoRequest request =
                (GetFeatureInfoRequest)
                        OwsUtils.parameter(operation.getParameters(), GetFeatureInfoRequest.class);
        GetFeatureInfoOutputFormat outputFormat = getRequestedOutputFormat(request);
        return outputFormat.getCharset();
    }
}
