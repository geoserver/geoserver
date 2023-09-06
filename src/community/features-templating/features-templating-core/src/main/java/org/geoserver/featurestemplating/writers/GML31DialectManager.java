/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.writers;

import java.io.IOException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;

class GML31DialectManager extends GMLDialectManager {

    GML31DialectManager(XMLStreamWriter streamWriter) {
        super(streamWriter, "posList");
    }

    @Override
    void writeNumberReturned(String numberReturned) throws XMLStreamException {
        streamWriter.writeAttribute("numberOfFeature", numberReturned);
    }

    @Override
    void writeNumberMatched(String numberMatched) throws XMLStreamException {}

    @Override
    void writeBoundingBox(ReferencedEnvelope envelope, CoordinateReferenceSystem crs)
            throws IOException {
        super.writeBoundingBox(envelope, crs, false);
    }

    @Override
    String getWfsNsUri() {
        return "http://www.opengis.net/wfs";
    }

    @Override
    String getGmlNsUri() {
        return "http://www.opengis.net/gml";
    }

    @Override
    void startFeatureMember() throws XMLStreamException {
        streamWriter.writeStartElement(GML_PREFIX, "featureMember", getGmlNsUri());
    }

    @Override
    void writeGeometryAttributes(int geomIndex) throws XMLStreamException {
        if (crs != null) {
            super.writeGeometryAttributes(geomIndex);
            streamWriter.writeAttribute(
                    "srsDimension", String.valueOf(crs.getCoordinateSystem().getDimension()));
        }
    }
}
