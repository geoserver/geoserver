/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSExtensions;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * Adapts all output formats able to encode GetFeatureInfo response . Allows to reuse all existing
 * WMS output formats in the OGC Maps API implementation.
 */
@Component
public class FeatureInfoResponseMessageConverter
        implements HttpMessageConverter<FeatureInfoResponse>, ApplicationContextAware {

    static final Logger LOGGER = Logging.getLogger(FeatureInfoResponseMessageConverter.class);
    private final WMS wms;
    private List<GetFeatureInfoOutputFormat> outputFormats;

    public FeatureInfoResponseMessageConverter(WMS wms) {
        this.wms = wms;
    }

    @Override
    public boolean canRead(Class<?> aClass, MediaType mediaType) {
        // write only
        return false;
    }

    @Override
    public boolean canWrite(Class<?> aClass, MediaType mediaType) {
        return FeatureInfoResponse.class.isAssignableFrom(aClass)
                && (mediaType == null
                        || getSupportedMediaTypes().stream()
                                .anyMatch(mt -> mt.isCompatibleWith(mediaType)));
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return outputFormats.stream()
                .map(
                        of -> {
                            try {
                                return MediaType.parseMediaType(of.getContentType());
                            } catch (InvalidMediaTypeException e) {
                                return null;
                            }
                        })
                .filter(mt -> mt != null)
                .collect(Collectors.toList());
    }

    @Override
    public FeatureInfoResponse read(
            Class<? extends FeatureInfoResponse> aClass, HttpInputMessage httpInputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(
            FeatureInfoResponse response, MediaType mediaType, HttpOutputMessage httpOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        GetFeatureInfoOutputFormat of =
                wms.getFeatureInfoOutputFormat(response.getRequest().getInfoFormat());
        of.write(response.getResult(), response.getRequest(), httpOutputMessage.getBody());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        outputFormats = WMSExtensions.findFeatureInfoFormats(applicationContext);
    }
}
