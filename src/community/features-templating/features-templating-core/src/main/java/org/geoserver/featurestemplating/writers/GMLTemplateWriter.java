/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.writers;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.util.ISO8601Formatter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;

/** A writer able to generate a GML output. */
public class GMLTemplateWriter extends XMLTemplateWriter {

    private GMLVersionManager versionManager;

    public GMLTemplateWriter(XMLStreamWriter streamWriter, String outputFormat) {
        super(streamWriter);
        TemplateIdentifier identifier =
                TemplateIdentifier.getTemplateIdentifierFromOutputFormat(outputFormat);
        switch (identifier) {
            case GML32:
                this.versionManager = new GML32VersionManager(streamWriter);
                break;
            case GML31:
                this.versionManager = new GML31VersionManager(streamWriter);
                break;
            case GML2:
                this.versionManager = new GML2VersionManager(streamWriter);
                break;
        }
    }

    @Override
    public void startTemplateOutput(Map<String, Object> encodingHints) throws IOException {
        try {
            streamWriter.writeStartDocument();
            streamWriter.writeStartElement("wfs", "FeatureCollection", namespaces.get("wfs"));

            Set<String> nsKeys = namespaces.keySet();
            for (String k : nsKeys) {
                streamWriter.writeNamespace(k, namespaces.get(k));
            }
            Set<String> xsiKeys = schemaLocations.keySet();
            for (String k : xsiKeys) {
                streamWriter.writeAttribute(k, schemaLocations.get(k));
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void endTemplateOutput(Map<String, Object> encodingHints) throws IOException {

        try {
            streamWriter.writeEndElement();
            streamWriter.writeEndDocument();
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void writeGeometry(Geometry writeGeometry) throws XMLStreamException {
        versionManager.writeGeometry(writeGeometry);
    }

    public void writeNumberReturned() throws IOException {
        try {
            versionManager.writeNumberReturned(String.valueOf(numberReturned));
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeCollectionCounts(BigInteger featureCount) throws IOException {
        String stringValue;
        if (featureCount != null && featureCount.longValue() >= 0)
            stringValue = String.valueOf(featureCount);
        else stringValue = "unknown";
        try {
            versionManager.writeNumberMatched(stringValue);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeCrs() throws IOException {
        try {
            streamWriter.writeAttribute(
                    "srsDimension", String.valueOf(crs.getCoordinateSystem().getDimension()));
            String crsIdentifier = getCRSIdentifier(crs);
            streamWriter.writeAttribute("srsName", crsIdentifier);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeCollectionBounds(ReferencedEnvelope envelope) throws IOException {
        versionManager.writeBoundingBox(envelope, crs);
    }

    public void writeTimeStamp() throws IOException {
        try {
            streamWriter.writeAttribute("timeStamp", new ISO8601Formatter().format(new Date()));
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void addNamespaces(Map<String, String> namespaces) {
        super.addNamespaces(namespaces);
        versionManager.addNamespaces(namespaces);
    }
}
