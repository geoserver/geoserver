/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.api;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import org.geoserver.gsr.model.GSRModel;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.MediaType;

/** Abstract class for all {@link GSRModel} converters */
public abstract class AbstractGeoServicesConverter extends BaseMessageConverter<GSRModel> {

    protected AbstractGeoServicesConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
    }

    protected AbstractGeoServicesConverter(
            Charset defaultCharset, MediaType... supportedMediaTypes) {
        super(defaultCharset, supportedMediaTypes);
    }

    /**
     * Serializes an object in a context-insensitive way, so that it can be called outside of the
     * regular request handling hierarchy. Mainly used for exception handling; see {@link
     * FormatParameterInterceptor} and {@link GeoServicesExceptionResolver}.
     *
     * @param os {@link OutputStream} to write to
     * @param o Object to write
     * @throws IOException
     */
    public abstract void writeToOutputStream(OutputStream os, Object o) throws IOException;
}
