/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util.patch;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxReader;
import java.io.InputStream;
import java.io.Reader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;

/**
 * JSON driver that preserves JSON null by injecting a synthetic CHARACTERS event between START_ELEMENT and END_ELEMENT
 * for empty elements.
 */
public class NullAwareJettisonMappedXmlDriver extends JettisonMappedXmlDriver {

    private final Configuration configuration;

    public NullAwareJettisonMappedXmlDriver(Configuration configuration, boolean useSerializeAsArray) {
        super(configuration, useSerializeAsArray);
        this.configuration = configuration;
    }

    @Override
    public HierarchicalStreamReader createReader(Reader reader) {
        try {
            XMLStreamReader xml = createXmlStreamReader(reader);
            return createStaxReader(xml);
        } catch (XMLStreamException e) {
            throw new StreamException(e);
        }
    }

    @Override
    public HierarchicalStreamReader createReader(InputStream input) {
        try {
            XMLStreamReader xml = createXmlStreamReader(input);
            return createStaxReader(xml);
        } catch (XMLStreamException e) {
            throw new StreamException(e);
        }
    }

    private XMLStreamReader createXmlStreamReader(Reader reader) throws XMLStreamException {
        XMLInputFactory f = new MappedXMLInputFactory(configuration);
        XMLStreamReader base = f.createXMLStreamReader(reader);
        return new NullMarkingStreamReader(base);
    }

    private XMLStreamReader createXmlStreamReader(InputStream input) throws XMLStreamException {
        XMLInputFactory f = new MappedXMLInputFactory(configuration);
        XMLStreamReader base = f.createXMLStreamReader(input);
        return new NullMarkingStreamReader(base);
    }

    private HierarchicalStreamReader createStaxReader(XMLStreamReader xml) {
        // Same as JettisonMappedXmlDriver: StaxReader over XMLStreamReader
        return new StaxReader(new QNameMap(), xml, getNameCoder());
    }
}
