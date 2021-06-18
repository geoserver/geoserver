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
        return "http://www.opengis.net/wfs/2.0";
    }

    @Override
    String getGmlNsUri() {
        return "http://www.opengis.net/gml/3.2";
    }

    @Override
    void startFeatureMember() throws XMLStreamException {
        streamWriter.writeStartElement(WFS_PREFIX, "member", getWfsNsUri());
    }

    @Override
    void writeGeometryAttributes(int geometryIndex) throws XMLStreamException {
        super.writeGeometryAttributes(geometryIndex);
        if (crs != null) {
            streamWriter.writeAttribute(
                    "srsDimension", String.valueOf(crs.getCoordinateSystem().getDimension()));
        }
        StringBuilder id = new StringBuilder("");
        if (typeName != null) {
            id.append(typeName).append(".");
            id.append(currentFeatureNumber).append(".geom");
        }
        if (geometryIndex > 0) id.append(".").append(geometryIndex);
        if (!"".equals(id.toString()))
            streamWriter.writeAttribute("gml", getGmlNsUri(), "id", id.toString());
    }
}
