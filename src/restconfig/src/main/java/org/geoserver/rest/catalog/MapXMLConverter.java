/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

@Component
public class MapXMLConverter extends BaseMessageConverter<Map<?, ?>> {

    public MapXMLConverter() {
        super(MediaType.TEXT_XML, MediaType.APPLICATION_XML);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return Map.class.isAssignableFrom(clazz) && !Properties.class.isAssignableFrom(clazz);
    }

    //
    // reading
    //
    @Override
    public Map<?, ?> readInternal(Class<? extends Map<?, ?>> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        Object result;
        SAXBuilder builder = new SAXBuilder();
        builder.setEntityResolver(catalog.getResourcePool().getEntityResolver());
        Document doc;
        try {
            doc = builder.build(inputMessage.getBody());
        } catch (JDOMException e) {
            throw new IOException("Error building document", e);
        }

        Element elem = doc.getRootElement();
        result = convert(elem);
        return (Map<?, ?>) result;
    }

    //
    // writing
    //
    @Override
    public void writeInternal(Map<?, ?> map, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        Element root = new Element(getMapName(map));
        final Document doc = new Document(root);
        insert(root, map);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(doc, outputMessage.getBody());
    }

    protected String getMapName(Map<?, ?> map) {
        if (map instanceof NamedMap) {
            return ((NamedMap<?, ?>) map).getName();
        } else {
            return "root";
        }
    }

    /**
     * Interpret XML and convert it back to a Java collection.
     *
     * @param elem a JDOM element
     * @return the Object produced by interpreting the XML
     */
    protected Object convert(Element elem) {
        List<?> children = elem.getChildren();
        if (children.size() == 0) {
            if (elem.getContent().size() == 0) {
                return null;
            } else {
                return elem.getText();
            }
        } else if (children.get(0) instanceof Element) {
            Element child = (Element) children.get(0);
            if (child.getName().equals("entry")) {
                List<Object> l = new ArrayList<>();
                for (Object o : elem.getChildren("entry")) {
                    Element curr = (Element) o;
                    l.add(convert(curr));
                }
                return l;
            } else {
                Map<String, Object> m = new NamedMap<>(child.getName());
                for (Object aChildren : children) {
                    Element curr = (Element) aChildren;
                    m.put(curr.getName(), convert(curr));
                }
                return m;
            }
        }
        throw new RuntimeException("Unable to parse XML");
    }

    /**
     * Generate the JDOM element needed to represent an object and insert it into the parent element
     * given.
     *
     * @todo This method is recursive and could cause stack overflow errors for large input maps.
     * @param elem the parent Element into which to insert the created JDOM element
     * @param object the Object to be converted
     */
    protected void insert(Element elem, Object object) {
        if (object instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) object;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Element newElem = new Element(entry.getKey().toString());
                insert(newElem, entry.getValue());
                elem.addContent(newElem);
            }
        } else if (object instanceof Collection) {
            Collection<?> collection = (Collection<?>) object;

            for (Object entry : collection) {
                Element newElem = new Element("entry");
                insert(newElem, entry);
                elem.addContent(newElem);
            }
        } else {
            elem.addContent(object == null ? "" : object.toString());
        }
    }
}
