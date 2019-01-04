/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.io.Reader;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geotools.util.Version;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class MessageXmlParser extends XmlRequestReader {
    public MessageXmlParser() {
        this(null, new Version("1.0.0"));
    }

    public MessageXmlParser(String namespace, Version ver) {
        super(new QName(namespace, "Hello"), ver, "hello");
    }

    public Object read(Object request, Reader reader, Map kvp) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document doc = builder.parse(new InputSource(reader));
        String message = doc.getDocumentElement().getAttribute("message");

        return new Message(message);
    }
}
