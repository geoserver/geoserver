/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.rest.format.StreamDataFormat;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.restlet.data.MediaType;

/**
 * A format that automatically converts a map into an XML document for access control rules and vice
 * versa.
 * 
 * <p>
 * The resulting XML document contains elements named <strong>{@value #RULEELEMENT}</strong> having
 * an attribute <strong>{@value #RESOURCEATTR}</strong> containing the resource to protect.
 * 
 * The content of these elements is a comma delimited list of role names.
 * </p>
 * 
 * @author christian
 */
public class RuleXMLFormat extends StreamDataFormat {

    final static String ROOTELEMENT = "rules";

    final static String RULEELEMENT = "rule";

    final static String RESOURCEATTR = "resource";

    public RuleXMLFormat() {
        super(MediaType.APPLICATION_XML);
    }

    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        Element root = new Element(ROOTELEMENT);
        final Document doc = new Document(root);
        insert(root, (Map<String, String>) object);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(doc, out);
    }

    /**
     * Generate the JDOM element needed to represent an access control rule and insert it into the
     * parent element given.
     *
     * @param elem
     *            , the root elment
     * @param ruleMap
     */

    private final void insert(Element elem, Map<String, String> ruleMap) {

        for (Map.Entry<String, String> entry : ruleMap.entrySet()) {
            Element ruleElement = new Element(RULEELEMENT);
            ruleElement.setAttribute(RESOURCEATTR, entry.getKey());
            ruleElement.setText(entry.getValue());
            elem.getChildren().add(ruleElement);
        }
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        Map<String, String> result = null;
        SAXBuilder builder = new SAXBuilder();
        Document doc;
        try {
            doc = builder.build(in);
        } catch (JDOMException e) {
            throw (IOException) new IOException("Error building document").initCause(e);
        }

        Element elem = doc.getRootElement();
        result = convert(elem);
        return result;
    }

    /**
     * Interpret XML and convert it back to a Map<String,String>
     *
     * @param elem a JDOM element
     * @return the Map<String,String> produced by interpreting the XML
     */
    private Map<String, String> convert(Element elem) {
        Map<String, String> ruleMap = new HashMap<String, String>();
        List<Element> children = elem.getChildren();
        for (Element ruleElement : children) {
            String resource = ruleElement.getAttributeValue(RESOURCEATTR);
            ruleMap.put(resource, ruleElement.getTextTrim());
        }
        return ruleMap;
    }
}
