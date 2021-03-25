/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.rest.converters.BaseMessageConverter;
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

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            XMLOutputFactory factory = XMLOutputFactory.newFactory();
            XMLStreamWriter writer = factory.createXMLStreamWriter(out, "UTF-8");
            writer.writeStartDocument();
            writer.writeStartElement("list");
            for (String name : t) {
                writer.writeStartElement(t.getName());
                writer.writeCharacters(name);
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndDocument();
        } catch (XMLStreamException | FactoryConfigurationError e) {
            throw new IOException(e);
        }
        // indent and write. If we didn't indent, the above could just write to
        // outputMessage.getBody()
        Transformer transformer;
        try (OutputStream os = outputMessage.getBody()) {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            Source source = new StreamSource(new ByteArrayInputStream(out.toByteArray()));
            transformer.transform(source, new StreamResult(os));
        } catch (TransformerException | TransformerFactoryConfigurationError e) {
            throw new IOException(e);
        }
    }
}
