/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/**
 * Outputs a named list of strings, as represented by {@link AvailableResources}.
 *
 * <p>This is used for WMS output.
 *
 * @author Kevin Smith (Boundless)
 */
// TODO: This is a duplicate of StringsListConverter
@Component
public class AvailableResourcesConverter extends BaseMessageConverter<AvailableResources> {

    // static final List<MediaType> MEDIA_TYPES = Arrays.asList(MediaType.APPLICATION_XML,
    // MediaType.APPLICATION_JSON);

    public AvailableResourcesConverter() {
        super(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return AvailableResources.class.isAssignableFrom(clazz);
    }

    @Override
    protected AvailableResources readInternal(
            Class<? extends AvailableResources> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException(
                "AvailableResourceConverter does not support deserialization", inputMessage);
    }

    @Override
    protected void writeInternal(
            AvailableResources availableResources, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        MediaType contentType = outputMessage.getHeaders().getContentType();

        if (MediaType.APPLICATION_XML.isCompatibleWith(contentType)) {
            writeXML(availableResources, outputMessage);
        } else if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            writeJSON(availableResources, outputMessage);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void writeJSON(AvailableResources t, HttpOutputMessage outputMessage)
            throws IOException {
        JSONArray names = new JSONArray();
        names.addAll(t);
        JSONObject string = new JSONObject();
        string.put("string", names);
        JSONObject root = new JSONObject();
        root.put("list", string);
        try (OutputStream os = outputMessage.getBody();
                Writer writer = new OutputStreamWriter(os)) {

            root.write(writer);
        }
    }

    protected void writeXML(AvailableResources t, HttpOutputMessage outputMessage)
            throws IOException {
        Element root = new Element("list");
        final Document doc = new Document(root);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());

        t.stream().map(name -> new Element(t.getName()).addContent(name)).forEach(root::addContent);

        try (OutputStream os = outputMessage.getBody()) {
            outputter.output(doc, os);
        }
    }
}
