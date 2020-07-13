/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.sf.json.*;
import org.apache.commons.io.IOUtils;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.gsr.model.feature.Feature;
import org.geoserver.gsr.model.feature.FeatureArray;
import org.geoserver.gsr.translate.feature.FeatureEncoder;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

@Component
public class GSRModelReader extends BaseMessageConverter<GSRModel> {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(GSRModelReader.class);

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
    protected GSRModel readInternal(Class<? extends GSRModel> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
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
                        LOGGER.log(
                                java.util.logging.Level.WARNING, "Error parsing json feature", e);
                    }
                }
                return new FeatureArray(features);
            }
        }
        throw new HttpMessageNotReadableException(
                this.getClass().getName() + " does not support deserialization", inputMessage);
    }
}
