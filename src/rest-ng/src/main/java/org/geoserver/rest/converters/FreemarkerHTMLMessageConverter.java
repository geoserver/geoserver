package org.geoserver.rest.converters;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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

    public FreemarkerHTMLMessageConverter() {
        super();
    }

    public FreemarkerHTMLMessageConverter(String encoding) {
        this();
        this.encoding = encoding;
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return MediaType.TEXT_HTML.includes(mediaType) && RestWrapper.class.isAssignableFrom(clazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.TEXT_HTML);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    /**
     * Write an given object to the given output message as HTML.
     *
     * @param o Object to serialize. Must be an instance of {@link RestWrapper}
     * @param contentType the content type to use when writing
     * @param outputMessage the message to write to
     * @throws IOException in case of I/O errors
     * @throws HttpMessageNotWritableException in case of conversion errors
     * @throws IllegalArgumentException if o is not an instance of {@link RestWrapper}
     */
    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Writer tmplWriter = null;
        if (o instanceof RestWrapper) {
            RestWrapper wrapper = (RestWrapper) o;
            wrapper.configureFreemarker(this);
            try {
                Object object = wrapper.getObject();
                Template template = wrapper.getTemplate();
                OutputStream outputStream = outputMessage.getBody();

                if (contentType.getCharSet() != null) {
                    tmplWriter = new BufferedWriter(
                            new OutputStreamWriter(outputStream, contentType.getCharSet().name()));
                } else {
                    tmplWriter = new BufferedWriter(
                            new OutputStreamWriter(outputStream, template.getEncoding()));
                }

                template.process(object, tmplWriter);
                tmplWriter.flush();
            } catch (TemplateException te) {
                throw new IOException("Template processing error " + te.getMessage());
            } finally {
                if (tmplWriter != null) {
                    tmplWriter.close();
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Object must be an instance of RestWrapper. Was: " + o.getClass());
        }
    }

    public int getPriority() {
        // If no extension or content-type provided, return HTML;
        return ExtensionPriority.LOWEST - 1;
    }

    public List<URL> createCollectionLink(String link) {
        // TODO Auto-generated method stub
        try {
            String href = href(link);
            URL url2 = new URL(href);
            return (List<URL>) Collections.singletonList(url2);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return new ArrayList<URL>();
    }

    protected String href(String link) {

        final RequestInfo pg = RequestInfo.get();
        String ext = "html";

        if (ext != null && ext.length() > 0)
            link = link + "." + ext;

        // encode as relative or absolute depending on the link type
        if (link.startsWith("/")) {
            // absolute, encode from "root"
            return pg.servletURI(link);
        } else {
            // encode as relative
            return pg.pageURI(link);
        }
    }
}
