/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.collections4.iterators.NodeListIterator;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

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

        Document dom;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            builder.setEntityResolver(catalog.getResourcePool().getEntityResolver());
            dom = builder.parse(inputMessage.getBody());
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new IOException("Error building document", e);
        }
        Element elem = dom.getDocumentElement();
        Object result = convert(elem);
        return (Map<?, ?>) result;
    }

    //
    // writing
    //
    @Override
    public void writeInternal(Map<?, ?> map, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {

        Element root;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.newDocument();
            root = doc.createElement(getMapName(map));
            doc.appendChild(root);
        } catch (ParserConfigurationException e) {
            throw new IOException("Error building document", e);
        }

        insert(root, map);
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            Result outputTarget = new StreamResult(outputMessage.getBody());
            transformer.transform(new DOMSource(root), outputTarget);
        } catch (TransformerException e) {
            throw new IOException("Error writing document", e);
        }
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
        final List<Element> children = getChildren(elem);
        if (children.isEmpty()) {
            String content = elem.getTextContent();
            if (null == content || content.isEmpty()) {
                return null;
            }
            return content;
        } else if (children.get(0) instanceof Element) {
            final Element child = children.get(0);
            if (child.getNodeName().equals("entry")) {
                List<Object> l = new ArrayList<>();
                for (Node n : children) {
                    if (!(n instanceof Element && "entry".equals(n.getNodeName()))) {
                        continue;
                    }
                    Element curr = (Element) n;
                    l.add(convert(curr));
                }
                return l;
            } else {
                Map<String, Object> m = new NamedMap<>(child.getNodeName());
                for (Element curr : children) {
                    m.put(curr.getNodeName(), convert(curr));
                }
                return m;
            }
        }
        throw new RuntimeException("Unable to parse XML");
    }

    private List<Element> getChildren(Element elem) {
        return Streams.stream(new NodeListIterator(elem))
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .collect(Collectors.toList());
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
                Element newElem = elem.getOwnerDocument().createElement(entry.getKey().toString());
                insert(newElem, entry.getValue());
                elem.appendChild(newElem);
            }
        } else if (object instanceof Collection) {
            Collection<?> collection = (Collection<?>) object;

            for (Object entry : collection) {
                Element newElem = elem.getOwnerDocument().createElement("entry");
                insert(newElem, entry);
                elem.appendChild(newElem);
            }
        } else {
            String text = object == null ? "" : object.toString();
            elem.setTextContent(text);
        }
    }
}
