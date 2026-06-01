/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.Reader;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geotools.util.NullEntityResolver;
import org.geotools.util.Version;
import org.geotools.xml.XMLUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/** XmlRequestReader used for testing, as such it is designed to access local xsd files. */
public class MessageXmlParser extends XmlRequestReader {
    public MessageXmlParser() {
        this(null, new Version("1.0.0"));
    }

    public MessageXmlParser(String namespace, Version ver) {
        super(new QName(namespace, "Hello"), ver, "hello");
    }

    @Override
    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        DocumentBuilderFactory dbf = XMLUtils.newDocumentBuilderFactory();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "all");
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "all");
        dbf.setExpandEntityReferences(false);
        dbf.setValidating(false);
        dbf.setNamespaceAware(true);

        DocumentBuilder builder = XMLUtils.newDocumentBuilder(dbf);
        builder.setEntityResolver(NullEntityResolver.INSTANCE);

        try {
            Document doc = builder.parse(new InputSource(reader));
            String message = doc.getDocumentElement().getAttribute("message");

            return new Message(message);
        } catch (Exception e) {
            throw cleanException(e);
        }
    }
}
