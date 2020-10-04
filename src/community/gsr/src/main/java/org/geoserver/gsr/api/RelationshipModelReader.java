/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.api;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import net.sf.json.*;
import org.apache.commons.io.IOUtils;
import org.geoserver.gsr.model.relationship.RelationshipClass;
import org.geoserver.gsr.model.relationship.RelationshipClassWrapper;
import org.geoserver.gsr.model.relationship.RelationshipModel;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

@Component
public class RelationshipModelReader extends BaseMessageConverter<RelationshipModel> {

    static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(RelationshipModelReader.class);

    public RelationshipModelReader() {
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    public int getPriority() {
        return super.getPriority() - 5;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return RelationshipModel.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean canWrite(MediaType mediaType) {
        return false;
    }

    /**
     * Converts the wrapped RelationshipClass object passed in as JSON to the controller into a
     * RelationshipClass object, avoiding XStream issues
     *
     * @param clazz Passed in object
     * @param inputMessage
     * @return
     * @throws IOException
     * @throws HttpMessageNotReadableException
     */
    @Override
    protected RelationshipModel readInternal(
            Class<? extends RelationshipModel> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(inputMessage.getBody(), bout);
        JSON json = JSONSerializer.toJSON(new String(bout.toByteArray()));
        if (RelationshipClassWrapper.class.isAssignableFrom(clazz)
                || RelationshipClass.class.isAssignableFrom(clazz)) {
            if (json instanceof JSONObject) {
                return new Gson().fromJson(String.valueOf(json), RelationshipClass.class);
            }
        }
        throw new HttpMessageNotReadableException(
                this.getClass().getName() + " does not support deserialization", inputMessage);
    }
}
