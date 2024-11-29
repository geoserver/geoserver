/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

public interface ResponseMessageConverter<T> extends HttpMessageConverter<T> {

    /**
     * Returns the list of supported media types given the value class, and the target value object
     * as well (to support generic {@link org.geoserver.ows.Response} objects gathering the output
     * media type from the response, like RawMap in WMS
     */
    List<MediaType> getSupportedMediaTypes(Class<?> valueClass, T value);

    boolean canWrite(Object value, MediaType mediaType);
}
