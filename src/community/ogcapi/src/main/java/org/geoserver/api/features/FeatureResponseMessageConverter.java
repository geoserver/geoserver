/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import org.geoserver.api.MessageConverterResponseAdapter;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.util.Version;
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

    private static final Version V2 = new Version("2.0");
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
        response.write(value.getResponse(), httpOutputMessage.getBody(), operation);
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
