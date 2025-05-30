/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;

/**
 * @author Chris Hodgson
 * @author prushforth
 */
public class MapMLMessageConverter extends BaseMessageConverter<Object> {

    @Autowired
    private MapMLEncoder mapMLEncoder;

    /** */
    public MapMLMessageConverter() {
        super(MapMLConstants.MAPML_MEDIA_TYPE);
    }

    /**
     * @param clazz
     * @param mediaType
     * @return
     */
    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
        return false;
    }

    /**
     * @param clazz
     * @param mediaType
     * @return
     */
    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        return canWrite(mediaType) && org.geoserver.mapml.xml.Mapml.class.isAssignableFrom(clazz);
    }

    /**
     * @param clazz
     * @return
     */
    @Override
    protected boolean supports(Class<?> clazz) {
        // should not be called, since we override canRead()/canWrite()
        throw new UnsupportedOperationException();
    }

    /**
     * @param o
     * @param outputMessage
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage)
            throws UnsupportedEncodingException, IOException {
        if (o instanceof org.geoserver.mapml.xml.Mapml) {
            // write to output based on global verbose setting
            boolean verbose = geoServer.getGlobal().getSettings().isVerbose();
            mapMLEncoder.encode((org.geoserver.mapml.xml.Mapml) o, outputMessage.getBody(), verbose);
        } else {
            throw new IllegalArgumentException("Can only write Mapml objects, got: " + o.getClass());
        }
    }
}
