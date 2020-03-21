package com.boundlessgeo.gsr.api;

import com.boundlessgeo.gsr.model.GSRModel;
import com.boundlessgeo.gsr.model.feature.Feature;
import com.boundlessgeo.gsr.model.feature.FeatureArray;
import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;
import net.sf.json.*;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Component
public class GSRModelReader extends BaseMessageConverter<GSRModel> {

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(GSRModelReader.class);

    public GSRModelReader() {
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return GSRModel.class.isAssignableFrom(clazz);
    }

    //
    // Reading
    //
    @Override
    protected boolean canWrite(MediaType mediaType) {
        return false;
    }

    //
    // writing
    //
    @Override
    protected GSRModel readInternal(Class<? extends GSRModel> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(inputMessage.getBody(), bout);
        JSON json = JSONSerializer.toJSON(new String(bout.toByteArray()));
        if (FeatureArray.class.isAssignableFrom(clazz)) {
            if (json instanceof JSONArray) {
                List<Feature> features = new ArrayList<>();
                JSONArray jsonArray = (JSONArray) json;
                for (Object o : jsonArray) {
                    try {
                        features.add(FeatureEncoder.fromJson((JSONObject) o));
                    } catch (JSONException e) {
                        features.add(null);
                        LOGGER.log(java.util.logging.Level.WARNING, "Error parsing json feature", e);
                    }
                }
                return new FeatureArray(features);
            }
        }
        throw new HttpMessageNotReadableException(this.getClass().getName() + " does not support deserialization");
    }


}
