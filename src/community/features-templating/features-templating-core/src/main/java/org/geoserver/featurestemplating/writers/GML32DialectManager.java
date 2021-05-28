package org.geoserver.featurestemplating.writers;

import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

class GML32DialectManager extends GMLDialectManager {

    GML32DialectManager(XMLStreamWriter streamWriter) {
        super(streamWriter, "posList");
    }

    @Override
    void writeNumberReturned(String numberReturned) throws XMLStreamException {
        streamWriter.writeAttribute("numberReturned", String.valueOf(numberReturned));
    }

    @Override
    void writeNumberMatched(String numberMatched) throws XMLStreamException {
        streamWriter.writeAttribute("numberMatched", String.valueOf(numberMatched));
    }

    @Override
    void writeBoundingBox(ReferencedEnvelope envelope, CoordinateReferenceSystem crs)
            throws IOException {
        super.writeBoundingBox(envelope, crs, true);
    }

    @Override
    String getWfsNsUri() {
        return "http://www.opengis.net/wfs";
    }

    @Override
    String getGmlNsUri() {
        return "http://www.opengis.net/gml/3.2";
    }
}
