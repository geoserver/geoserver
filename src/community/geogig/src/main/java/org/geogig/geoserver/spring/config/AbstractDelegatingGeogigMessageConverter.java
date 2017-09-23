/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.config;

import java.io.IOException;

import org.geogig.geoserver.spring.dto.RepositoryImportRepo;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.locationtech.geogig.spring.dto.RepositoryInitRepo;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Abstract implementation for a delegating http message converter that applies only to classes in geogig DTO packages.
 */
public abstract class AbstractDelegatingGeogigMessageConverter extends BaseMessageConverter<Object> {
    
    private final HttpMessageConverter<Object> delegate;
    
    public AbstractDelegatingGeogigMessageConverter(HttpMessageConverter<Object> delegate, MediaType... mediaTypes) {
        super(delegate.getSupportedMediaTypes().toArray(new MediaType[delegate.getSupportedMediaTypes().size()]));
        this.delegate = delegate;
    }
    
    @SuppressWarnings("rawtypes")
    private boolean isGeogigPackage(Class clazz) {
        return clazz.getPackage().equals(RepositoryImportRepo.class.getPackage()) || clazz.getPackage().equals(RepositoryInitRepo.class.getPackage());
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected boolean supports(Class clazz) {
        return isGeogigPackage(clazz);
    }
    
    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return isGeogigPackage(clazz) && delegate.canRead(clazz, mediaType);
    }
    
    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return isGeogigPackage(clazz) && delegate.canWrite(clazz, mediaType);
    }
    
    @Override
    protected Object readInternal(Class<? extends Object> clazz,
            HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return delegate.read(clazz, inputMessage);
    }
    
    @Override
    protected void writeInternal(Object object, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        delegate.write(object, outputMessage.getHeaders().getContentType(), outputMessage);
    }
    
    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }
}

