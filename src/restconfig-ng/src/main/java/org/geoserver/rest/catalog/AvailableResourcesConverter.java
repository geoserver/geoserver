package org.geoserver.rest.catalog;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.geoserver.rest.converters.BaseMessageConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Component
public class AvailableResourcesConverter extends BaseMessageConverter {
    
    static final List<MediaType> MEDIA_TYPES = Arrays.asList(MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON);
    
    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class clazz, final MediaType mediaType) {
        return AvailableResources.class.isAssignableFrom(clazz) && MEDIA_TYPES.stream()
                .anyMatch(type->type.isCompatibleWith(mediaType));
    }

    @Override
    public List getSupportedMediaTypes() {
        return MEDIA_TYPES;
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException("AvailableResourceConverter does not support deserialization");
    }
    
    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        if(MediaType.APPLICATION_XML.isCompatibleWith(contentType)) {
            writeXML((AvailableResources)t, outputMessage);
        } else if(MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            writeJSON((AvailableResources)t, outputMessage);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private void writeJSON(AvailableResources t, HttpOutputMessage outputMessage) throws IOException {
        JSONArray names = new JSONArray();
        names.addAll(t);
        JSONObject string = new JSONObject();
        string.put("string", names);
        JSONObject root = new JSONObject();
        root.put("list", string);
        try(OutputStream os = outputMessage.getBody();
            Writer writer = new OutputStreamWriter(os)) {
            
            root.write(writer);
        }
    }

    protected void writeXML(AvailableResources t, HttpOutputMessage outputMessage) throws IOException {
        Element root = new Element("list");
        final Document doc = new Document(root);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        
        ((AvailableResources) t).stream()
            .map(name-> new Element(((AvailableResources) t).getName()).addContent(name))
            .forEach(element-> root.addContent(element));
        
        try(OutputStream os = outputMessage.getBody()) {
            outputter.output(doc, os);
        }
    }

}
