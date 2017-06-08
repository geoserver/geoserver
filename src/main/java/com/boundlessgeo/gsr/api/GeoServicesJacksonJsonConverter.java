package com.boundlessgeo.gsr.api;

import com.boundlessgeo.gsr.core.GSRModel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * JSON converter using jackson
 */
@Component
public class GeoServicesJacksonJsonConverter extends BaseMessageConverter<GSRModel> {

    ObjectMapper mapper = new ObjectMapper();

    public GeoServicesJacksonJsonConverter() {
        super(MediaType.APPLICATION_JSON);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return GSRModel.class.isAssignableFrom(clazz);
    }

    @Override
    public GSRModel readInternal(Class<? extends GSRModel> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return mapper.readValue(inputMessage.getBody(), clazz);
    }

    @Override
    public void writeInternal(GSRModel o, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        mapper.writerWithDefaultPrettyPrinter().writeValue(outputMessage.getBody(), o);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
