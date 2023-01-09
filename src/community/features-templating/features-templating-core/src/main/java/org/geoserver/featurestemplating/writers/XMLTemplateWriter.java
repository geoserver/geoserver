/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.writers;

import static org.geoserver.featurestemplating.builders.EncodingHints.ENCODE_AS_ATTRIBUTE;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.lang.StringEscapeUtils;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
import org.geoserver.featurestemplating.builders.EncodingHints;
import org.geoserver.util.ISO8601Formatter;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;

/** A writer capable to generate a generic xml result. */
public abstract class XMLTemplateWriter extends TemplateOutputWriter {

    protected XMLStreamWriter streamWriter;

    protected Map<String, String> namespaces = new HashMap<>();

    protected String schemaLocations;

    public XMLTemplateWriter(XMLStreamWriter streamWriter) {
        this.streamWriter = streamWriter;
    }

    @Override
    public void writeElementName(Object elementName, EncodingHints encodingHints)
            throws IOException {
        try {
            String elemName = elementName.toString();
            String[] elems = elemName.split(":");
            if (elems.length > 1)
                streamWriter.writeStartElement(elems[0], elems[1], namespaces.get(elems[0]));
            else streamWriter.writeStartElement(elems[0]);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeElementValue(Object elementValue, EncodingHints encodingHints)
            throws IOException {

        writeElementNameAndValue(null, elementValue, encodingHints);
    }

    protected abstract void writeGeometry(Geometry writeGeometry) throws XMLStreamException;

    @Override
    public void writeStaticContent(String name, Object staticContent, EncodingHints encodingHints)
            throws IOException {
        try {
            if (isEncodeAsAttribute(encodingHints))
                writeAsAttribute(name, staticContent, encodingHints);
            else {
                streamWriter.writeStartElement(name);
                evaluateChildren(encodingHints);
                streamWriter.writeCharacters(
                        StringEscapeUtils.escapeHtml(staticContent.toString()));
                streamWriter.writeEndElement();
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void startObject(String name, EncodingHints encodingHints) throws IOException {
        writeElementName(name, encodingHints);
    }

    @Override
    public void endObject(String name, EncodingHints encodingHints) throws IOException {
        try {
            streamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void startArray(String name, EncodingHints encodingHints) throws IOException {
        writeElementName(name, encodingHints);
    }

    @Override
    public void endArray(String name, EncodingHints encodingHints) throws IOException {
        try {
            streamWriter.writeEndElement();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            streamWriter.close();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeElementNameAndValue(
            String key, Object elementValue, EncodingHints encodingHints) throws IOException {
        boolean encodeAsAttribute = isEncodeAsAttribute(encodingHints);
        boolean repeatName = elementValue instanceof List && ((List) elementValue).size() > 1;
        boolean canClose = false;
        if (key != null && !repeatName && !encodeAsAttribute) {
            writeElementName(key, encodingHints);
            evaluateChildren(encodingHints);
        }
        try {
            if (isNull(elementValue)) {
                String value = "";
                if (encodeAsAttribute) writeAsAttribute(key, value, encodingHints);
                else {
                    writeAsCharacters(value);
                    canClose = true;
                }
            } else if (elementValue instanceof String
                    || elementValue instanceof Number
                    || elementValue instanceof Boolean
                    || elementValue instanceof URI
                    || elementValue instanceof URL) {
                if (encodeAsAttribute) writeAsAttribute(key, elementValue, encodingHints);
                else {
                    writeAsCharacters(String.valueOf(elementValue));
                    canClose = true;
                }
            } else if (elementValue instanceof Geometry) {
                writeGeometry((Geometry) elementValue);
                canClose = true;
            } else if (elementValue instanceof Date) {
                Date timeStamp = (Date) elementValue;
                String formatted = new ISO8601Formatter().format(timeStamp);
                if (encodeAsAttribute) writeAsAttribute(key, elementValue, encodingHints);
                else {
                    streamWriter.writeCharacters(formatted);
                    canClose = true;
                }
            } else if (elementValue instanceof ComplexAttribute) {
                ComplexAttribute attr = (ComplexAttribute) elementValue;
                writeElementNameAndValue(
                        encodeAsAttribute ? key : null, attr.getValue(), encodingHints);
            } else if (elementValue instanceof Attribute) {
                Attribute attr = (Attribute) elementValue;
                writeElementNameAndValue(
                        encodeAsAttribute ? key : null, attr.getValue(), encodingHints);
            } else if (elementValue instanceof List) {
                List list = (List) elementValue;
                if (!repeatName && !list.isEmpty()) {
                    writeElementNameAndValue(key, list.get(0), encodingHints);
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        writeElementNameAndValue(key, list.get(i), encodingHints);
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
        if (canClose) {
            endObject(null, null);
        }
    }

    private void writeAsCharacters(String value) throws XMLStreamException {
        if (value == null) value = "";
        value = escape(value);
        streamWriter.writeCharacters(value);
    }

    /**
     * Escape reserved xml characters in string.
     *
     * @param value the string to escape.
     * @return the string value with escaped xml.
     */
    protected String escape(String value) {
        return StringEscapeUtils.escapeXml(value);
    }

    private void writeAsAttribute(String key, Object elementValue, EncodingHints encodingHints)
            throws IOException {
        try {
            String strVal = escape(elementValue.toString());
            if (key.indexOf(":") != -1 && !key.contains("xmlns")) {
                String[] splitKey = key.split(":");
                streamWriter.writeAttribute(
                        splitKey[0], namespaces.get(splitKey[0]), splitKey[1], strVal);
            } else {
                if (key.contains("xmlns")) {
                    streamWriter.writeNamespace(key.split(":")[1], strVal);
                } else {
                    streamWriter.writeAttribute(key, strVal);
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    public void addNamespaces(Map<String, String> namespaces) {
        this.namespaces.putAll(namespaces);
    }

    public void addSchemaLocations(String schemaLocation) {
        if (schemaLocations != null) schemaLocations += " ".concat(schemaLocation);
        else this.schemaLocations = schemaLocation;
    }

    private boolean isEncodeAsAttribute(EncodingHints encodingHints) {
        boolean result = false;
        Boolean encodeAsAttribute =
                getEncodingHintIfPresent(encodingHints, ENCODE_AS_ATTRIBUTE, Boolean.class);
        if (encodeAsAttribute != null) result = encodeAsAttribute.booleanValue();
        return result;
    }

    private void evaluateChildren(EncodingHints encodingHints) throws IOException {
        AbstractTemplateBuilder.ChildrenEvaluation eval =
                encodingHints.get(
                        EncodingHints.CHILDREN_EVALUATION,
                        AbstractTemplateBuilder.ChildrenEvaluation.class);
        if (eval != null) {
            eval.evaluate();
        }
    }
}
