/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.api.MessageConverterResponseAdapter;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.springframework.http.HttpOutputMessage;
import org.springframework.stereotype.Component;

/**
 * Adapts all output formats able to encode a WFS {@link FeatureCollectionResponse} to a {@link
 * org.springframework.http.converter.HttpMessageConverter} encoding a {@link
 * org.geoserver.wfs.response.FeatureResponse}. Allows to reuse all existing WFS output formats in
 * the OGC Features API implementation.
 */
@Component
public class FeatureResponseMessageConverter
        extends MessageConverterResponseAdapter<FeaturesResponse> {

    static final Logger LOGGER = Logging.getLogger(FeatureResponseMessageConverter.class);
    private static final Version V2 = new Version("2.0");
    /** Name of the header stating the CRS of the response */
    public static final String CRS_RESPONSE_HEADER = "OGC-CRS";

    List<Response> responses;

    public FeatureResponseMessageConverter() {
        super(FeaturesResponse.class, FeatureCollectionResponse.class);
    }

    @Override
    protected void writeResponse(
            FeaturesResponse value,
            HttpOutputMessage httpOutputMessage,
            Operation operation,
            Response response)
            throws IOException {
        setHeaders(value.getResponse(), operation, response, httpOutputMessage);
        response.write(value.getResponse(), httpOutputMessage.getBody(), operation);
    }

    @Override
    protected void setHeaders(
            Object result, Operation operation, Response response, HttpOutputMessage message) {
        super.setHeaders(result, operation, response, message);
        // CRS extension, set CRS and axis order
        CoordinateReferenceSystem crs =
                Optional.ofNullable((FeatureCollectionResponse) result)
                        .map(fct -> fct.getFeatures().get(0))
                        .map(fc -> fc.getSchema())
                        .map(ft -> ft.getCoordinateReferenceSystem())
                        .orElse(null);
        if (crs != null) {
            try {
                String crsURI = getCRSURI(crs);
                String orderSpec = getOrderSpecification(crs);
                if (crsURI != null && orderSpec != null) {
                    message.getHeaders()
                            .set(CRS_RESPONSE_HEADER, crsURI + "; axisOrder=" + orderSpec);
                }
            } catch (FactoryException e) {
                LOGGER.log(
                        Level.INFO,
                        "Failed to lookup EPSG code of CRS, won't set the OGC-CRS header." + crs,
                        e);
            }
        }
    }

    private String getCRSURI(CoordinateReferenceSystem crs) throws FactoryException {
        if (CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) {
            return FeatureService.DEFAULT_CRS;
        }
        Integer code = CRS.lookupEpsgCode(crs, false);
        return FeatureService.CRS_PREFIX + code;
    }

    private String getOrderSpecification(CoordinateReferenceSystem crs) {
        CRS.AxisOrder order = CRS.getAxisOrder(crs);
        CoordinateSystemAxis a0 = crs.getCoordinateSystem().getAxis(0);
        CoordinateSystemAxis a1 = crs.getCoordinateSystem().getAxis(1);
        String orderSpec;
        if (order == CRS.AxisOrder.EAST_NORTH) {
            orderSpec = a0.getAbbreviation() + "," + a1.getAbbreviation();
        } else if (order == CRS.AxisOrder.NORTH_EAST) {
            orderSpec = a1.getAbbreviation() + "," + a0.getAbbreviation();
        } else {
            orderSpec = null;
        }
        return orderSpec;
    }

    @Override
    protected Operation getOperation(FeaturesResponse featuresResponse, Request dr) {
        return new Operation(
                dr.getOperation().getId(),
                dr.getOperation().getService(),
                dr.getOperation().getMethod(),
                new Object[] {featuresResponse.getRequest()});
    }

    @Override
    protected Predicate<Response> getResponseFilterPredicate() {
        return r ->
                r instanceof WFSGetFeatureOutputFormat
                        && ((WFSGetFeatureOutputFormat) r).canHandle(V2);
    }
}
