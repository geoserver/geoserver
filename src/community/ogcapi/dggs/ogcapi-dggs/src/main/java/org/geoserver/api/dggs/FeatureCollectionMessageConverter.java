/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.dggs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.ResultTypeType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.api.APIRequestInfo;
import org.geoserver.api.MessageConverterResponseAdapter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class FeatureCollectionMessageConverter
        extends MessageConverterResponseAdapter<SimpleFeatureCollection> {

    public FeatureCollectionMessageConverter() {
        super(SimpleFeatureCollection.class, FeatureCollectionResponse.class);
    }

    @Override
    public boolean canWrite(Class<?> aClass, MediaType mediaType) {
        return super.canWrite(aClass, mediaType);
    }

    @Override
    protected void writeResponse(
            SimpleFeatureCollection value,
            HttpOutputMessage httpOutputMessage,
            Operation operation,
            Response response)
            throws IOException {
        FeatureCollectionResponse fcr =
                FeatureCollectionResponse.adapt(
                        Wfs20Factory.eINSTANCE.createFeatureCollectionType());
        fcr.setFeatures(Arrays.asList(value));
        response.write(fcr, httpOutputMessage.getBody(), wrapOperation(operation));
    }

    @Override
    public Optional<Response> getResponse(MediaType mediaType) {
        Operation originalOperation = Dispatcher.REQUEST.get().getOperation();
        Operation op = wrapOperation(originalOperation);
        return responses
                .stream()
                .filter(r -> getMediaTypeStream(r).anyMatch(mt -> mediaType.isCompatibleWith(mt)))
                .filter(r -> r.canHandle(op))
                .findFirst();
    }

    public Operation wrapOperation(Operation originalOperation) {

        GetFeatureType request = Wfs20Factory.eINSTANCE.createGetFeatureType();
        request.setResultType(ResultTypeType.RESULTS);
        request.setBaseUrl(APIRequestInfo.get().getBaseURL());
        Operation op =
                new Operation(
                        "GetFeature",
                        originalOperation.getService(),
                        originalOperation.getMethod(),
                        new Object[] {request});
        return op;
    }
}
