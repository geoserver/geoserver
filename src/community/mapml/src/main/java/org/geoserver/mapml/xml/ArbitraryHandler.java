/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml.xml;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.DomHandler;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class ArbitraryHandler implements DomHandler<String, StreamResult> {

    @Override
    public StreamResult createUnmarshaller(ValidationEventHandler errorHandler) {
        return new StreamResult(new StringWriter());
    }

    @Override
    public String getElement(StreamResult sr) {
        return sr.getWriter().toString();
    }

    @Override
    public Source marshal(String str, ValidationEventHandler errorHandler) {
        return new StreamSource(new StringReader(str));
    }
}
