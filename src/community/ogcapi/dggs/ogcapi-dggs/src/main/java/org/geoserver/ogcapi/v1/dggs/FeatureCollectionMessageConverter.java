/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.dggs;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import javax.xml.namespace.QName;
import net.opengis.wfs20.GetFeatureType;
import net.opengis.wfs20.ResultTypeType;
import net.opengis.wfs20.Wfs20Factory;
import org.geoserver.ogcapi.APIRequestInfo;
import org.geoserver.ogcapi.MessageConverterResponseAdapter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geoserver.wfs.request.GetFeatureRequest;
import org.geoserver.wfs.request.Query;
import org.geotools.api.feature.simple.SimpleFeatureType;
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
        Operation wrappedOperation = wrapOperation(value, operation);
        FeatureCollectionResponse fcr =
                FeatureCollectionResponse.adapt(
                        Wfs20Factory.eINSTANCE.createFeatureCollectionType());
        fcr.setFeatures(Arrays.asList(value));
        setHeaders(fcr, wrappedOperation, response, httpOutputMessage);
        response.write(fcr, httpOutputMessage.getBody(), wrappedOperation);
    }

    @Override
    public Optional<Response> getResponse(MediaType mediaType) {
        Operation originalOperation = Dispatcher.REQUEST.get().getOperation();
        Operation op = wrapOperation(null, originalOperation);
        return responses.stream()
                .filter(r -> getMediaTypeStream(r).anyMatch(mt -> mediaType.isCompatibleWith(mt)))
                .filter(r -> r.canHandle(op))
                .findFirst();
    }

    public Operation wrapOperation(SimpleFeatureCollection fc, Operation originalOperation) {
        // bridge over expectations from the WFS subsystem
        GetFeatureType rawRequest = Wfs20Factory.eINSTANCE.createGetFeatureType();
        rawRequest.setResultType(ResultTypeType.RESULTS);
        rawRequest.setBaseUrl(APIRequestInfo.get().getBaseURL());
        if (fc != null) {
            GetFeatureRequest request = GetFeatureRequest.adapt(rawRequest);
            Query query = request.createQuery();
            SimpleFeatureType schema = fc.getSchema();
            query.setTypeNames(Arrays.asList(new QName(null, schema.getTypeName())));
            request.getAdaptedQueries().add(query.getAdaptee());
        }
        Operation op =
                new Operation(
                        "GetFeature",
                        originalOperation.getService(),
                        originalOperation.getMethod(),
                        new Object[] {rawRequest});
        return op;
    }
}
