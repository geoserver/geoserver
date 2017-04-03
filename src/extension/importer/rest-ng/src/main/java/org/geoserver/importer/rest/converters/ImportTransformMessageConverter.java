/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest.converters;

import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * {@link BaseMessageConverter} implementation for reading and writing {@link ImportTransform} objects to/from json
 */
@Component
public class ImportTransformMessageConverter extends BaseMessageConverter
        implements HttpMessageConverter {

    @Autowired
    private Importer importer;

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return ImportTransform.class.isAssignableFrom(clazz)
                || TransformChain.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return ImportTransform.class.isAssignableFrom(clazz)
                || TransformChain.class.isAssignableFrom(clazz);
    }

    @Override
    public List getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return newReader(inputMessage.getBody()).transform();
    }

    @Override
    public void write(Object object, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        ImportContextJSONConverterWriter io = newWriter(outputMessage.getBody());
        if (object instanceof TransformChain) {
            io.transformChain(task(), true, expand(1));
        } else {
            ImportTransform tx = (ImportTransform) object;
            ImportTask task = task();
            int index = task.getTransform().getTransforms().indexOf(tx);

            io.transform(tx, index, task(), true, expand(1));
        }
    }

    protected ImportTask task() {
        return task(false);
    }

    private String getAttribute(String name) {
        return (String) RequestContextHolder.getRequestAttributes().getAttribute(name,
                RequestAttributes.SCOPE_REQUEST);
    }

    protected ImportTask task(boolean optional) {
        ImportContext context = context();
        ImportTask task = null;

        String t = getAttribute("task");

        if (t != null) {
            int id = Integer.parseInt(t);
            task = context.task(id);
        }
        if (t != null && task == null) {
            throw new RestException("No such task: " + t + " for import: " + context.getId(),
                    HttpStatus.NOT_FOUND);
        }

        if (task == null && !optional) {
            throw new RestException("No task specified", HttpStatus.NOT_FOUND);
        }

        return task;
    }

    protected ImportContext context() {
        return context(false);
    }

    protected ImportContext context(boolean optional) {
        long i = Long.parseLong(getAttribute("import"));

        ImportContext context = importer.getContext(i);
        if (!optional && context == null) {
            throw new RestException("No such import: " + i, HttpStatus.NOT_FOUND);
        }
        return context;
    }

    protected int expand(int def) {
        String ex = getAttribute("expand");
        if (ex == null) {
            return def;
        }

        try {
            return "self".equalsIgnoreCase(ex) ? 1
                    : "all".equalsIgnoreCase(ex) ? Integer.MAX_VALUE
                            : "none".equalsIgnoreCase(ex) ? 0 : Integer.parseInt(ex);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    protected ImportContextJSONConverterReader newReader(InputStream input) throws IOException {
        return new ImportContextJSONConverterReader(importer, input);
    }

    protected ImportContextJSONConverterWriter newWriter(OutputStream output) throws IOException {
        return new ImportContextJSONConverterWriter(importer, output);
    }

}
