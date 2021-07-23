/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.writers;

import static org.geoserver.featurestemplating.builders.VendorOptions.LINK;
import static org.geoserver.featurestemplating.builders.VendorOptions.SCRIPT;
import static org.geoserver.featurestemplating.builders.VendorOptions.STYLE;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.WKTWriter2;
import org.locationtech.jts.geom.Geometry;

/** A template writer able to produce XHTML output. */
public class XHTMLTemplateWriter extends XMLTemplateWriter {

    public XHTMLTemplateWriter(XMLStreamWriter streamWriter) {
        super(streamWriter);
    }

    @Override
    protected void writeGeometry(Geometry writeGeometry) throws XMLStreamException {
        WKTWriter2 wktWriter2 = new WKTWriter2();
        String wktGeom = wktWriter2.write(writeGeometry);
        if (writeGeometry.getSRID() > 0) {
            wktGeom = "SRID=" + writeGeometry.getSRID() + ";" + wktGeom;
        }
        streamWriter.writeCharacters(wktGeom);
    }

    @Override
    public void startTemplateOutput(EncodingHints encodingHints) throws IOException {
        try {
            streamWriter.writeStartElement("html");
            streamWriter.writeStartElement("head");
            List script = encodingHints.get(SCRIPT, List.class);
            if (script != null) encodeHeadContent(SCRIPT, script);
            List style = encodingHints.get(STYLE, List.class);
            if (style != null) encodeHeadContent(STYLE, style);
            encodeLinks(encodingHints);
            streamWriter.writeEndElement();
            streamWriter.writeStartElement("body");
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private void encodeHeadContent(String elementName, List contents) throws XMLStreamException {
        if (!contents.isEmpty()) {
            for (Object content : contents) {
                streamWriter.writeStartElement(elementName);
                if (content != null) streamWriter.writeCharacters(content.toString());
                streamWriter.writeEndElement();
            }
        }
    }

    private void encodeLinks(EncodingHints encodingHints) throws XMLStreamException {
        if (encodingHints != null) {
            Set<String> keys = encodingHints.keySet();
            for (String key : keys) {
                if (key.startsWith(LINK)) {
                    List linkAttrs = encodingHints.get(key, List.class);
                    if (!linkAttrs.isEmpty()) streamWriter.writeStartElement(LINK);
                    for (Object o : linkAttrs) {
                        if (o instanceof Attribute) {
                            Attribute attribute = ((Attribute) o);
                            QName name = attribute.getName();
                            streamWriter.writeAttribute(name.getLocalPart(), attribute.getValue());
                        }
                    }
                    if (!linkAttrs.isEmpty()) streamWriter.writeEndElement();
                }
            }
        }
    }

    @Override
    public void endTemplateOutput(EncodingHints encodingHints) throws IOException {
        try {
            streamWriter.writeEndElement();
            streamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeCollectionCounts(BigInteger featureCount) throws IOException {}

    @Override
    public void writeCrs() throws IOException {}

    @Override
    public void writeCollectionBounds(ReferencedEnvelope bounds) throws IOException {}

    @Override
    public void writeTimeStamp() throws IOException {}

    @Override
    public void writeNumberReturned() throws IOException {}
}
