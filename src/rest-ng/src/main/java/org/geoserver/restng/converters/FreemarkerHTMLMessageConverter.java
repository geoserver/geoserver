package org.geoserver.restng.converters;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.geoserver.restng.wrapper.FreemarkerConfigurationWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Message converter for Freemarker-generated HTML output
 */
public class FreemarkerHTMLMessageConverter extends BaseMessageConverter {

    private static final Logger LOGGER = Logging.getLogger("org.geoserver.restng.converters");

    /**
     * Encoding (null for default)
     */
    protected String encoding;

    public FreemarkerHTMLMessageConverter(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    public FreemarkerHTMLMessageConverter(ApplicationContext applicationContext, String encoding) {
        this(applicationContext);
        this.encoding = encoding;
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return MediaType.TEXT_HTML.equals(mediaType)
            && FreemarkerConfigurationWrapper.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.TEXT_HTML);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    /**
     * Write an given object to the given output message as HTML.
     *
     * @param o Object to serialize. Must be an instance of {@link FreemarkerConfigurationWrapper}
     * @param contentType the content type to use when writing
     * @param outputMessage the message to write to
     * @throws IOException in case of I/O errors
     * @throws HttpMessageNotWritableException in case of conversion errors
     * @throws IllegalArgumentException if o is not an instance of {@link FreemarkerConfigurationWrapper}
     */
    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        Writer tmplWriter = null;
        if (o instanceof FreemarkerConfigurationWrapper) {
            FreemarkerConfigurationWrapper wrapper = (FreemarkerConfigurationWrapper) o;

            try {
                Object object = wrapper.getObject();
                Template template = wrapper.getTemplate();
                OutputStream outputStream = outputMessage.getBody();

                if (contentType.getCharSet() != null) {
                    tmplWriter = new BufferedWriter(new OutputStreamWriter(
                            outputStream, contentType.getCharSet().name()));
                } else {
                    tmplWriter = new BufferedWriter(new OutputStreamWriter(
                            outputStream, template.getEncoding()));
                }

                template.process(object, tmplWriter);
                tmplWriter.flush();
            } catch (TemplateException te) {
                throw new IOException("Template processing error "
                        + te.getMessage());
            } finally {
                if (tmplWriter != null) {
                    tmplWriter.close();
                }
            }
        } else {
            throw new IllegalArgumentException("Object must be an instance of FreemarkerConfigurationWrapper. Was: "+o.getClass());
        }
    }
}
