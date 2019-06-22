package org.geoserver.api.features;

import org.geoserver.api.MessageConverterResponseAdapter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.DefaultWebFeatureService20;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.util.Version;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class FeatureResponseMessageConverter extends MessageConverterResponseAdapter<FeaturesResponse> {

    private static final Version V2 = new Version("2.0");
    List<Response> responses;
    private List<MediaType> supportedMediaTypes;

    public FeatureResponseMessageConverter() {
        super(FeaturesResponse.class, FeatureCollectionResponse.class);
    }


    @Override
    protected void writeResponse(FeaturesResponse value, HttpOutputMessage httpOutputMessage, Operation operation, Response response) throws IOException {
        response.write(value.getResponse(), httpOutputMessage.getBody(), operation);
    }

    @Override
    protected Operation getOperation(FeaturesResponse featuresResponse, Request dr) {
        return new Operation(dr.getOperation().getId(), dr.getOperation().getService(), dr.getOperation().getMethod(), new Object[]{featuresResponse.getRequest()});
    }

    @Override
    protected Predicate<Response> getResponseFilterPredicate() {
        return r -> r instanceof WFSGetFeatureOutputFormat
                && ((WFSGetFeatureOutputFormat) r).canHandle(V2);
    }



}
