package org.geoserver.data.geogit;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.geogit.storage.ObjectWriter;
import org.geotools.referencing.CRS;
import org.geotools.referencing.wkt.Formattable;
import org.gvsig.bxml.stream.BxmlFactoryFinder;
import org.gvsig.bxml.stream.BxmlStreamWriter;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.google.common.base.Preconditions;

public class SimpleFeatureTypeWriter implements ObjectWriter<SimpleFeatureType> {

    private final SimpleFeatureType type;

    public SimpleFeatureTypeWriter(final SimpleFeatureType type) {
        Preconditions.checkNotNull(type);
        this.type = type;
    }

    @Override
    public void write(final OutputStream out) throws IOException {

        BxmlStreamWriter writer = BxmlFactoryFinder.newOutputFactory().createSerializer(out);
        try {
            writeInternal(writer);
        } finally {
            writer.close();
        }
    }

    private void writeInternal(final BxmlStreamWriter w) throws IOException {

        w.writeStartDocument();
        w.writeStartElement("", "type");
        w.writeStartAttribute("", "class");
        w.writeValue("SimpleFeatureType");
        w.writeEndAttributes();

        final List<AttributeDescriptor> descriptors = type.getAttributeDescriptors();
        for (AttributeDescriptor descriptor : descriptors) {
            w.writeStartElement("", "attribute");

            boolean nillable = descriptor.isNillable();
            w.writeStartAttribute("", "nillable");
            w.writeValue(nillable);

            Name name = descriptor.getName();
            w.writeStartAttribute("", "namespace");
            w.writeValue(name.getNamespaceURI() == null ? "" : name.getNamespaceURI());
            w.writeStartAttribute("", "name");
            w.writeValue(name.getLocalPart());

            int maxOccurs = descriptor.getMaxOccurs();
            w.writeStartAttribute("", "maxOccurs");
            w.writeValue(maxOccurs);

            int minOccurs = descriptor.getMinOccurs();
            w.writeStartAttribute("", "minOccurs");
            w.writeValue(minOccurs);

            w.writeEndAttributes();

            AttributeType attributeType = descriptor.getType();
            {
                w.writeStartElement("", "type");

                Name typeName = attributeType.getName();

                w.writeStartAttribute("", "namespace");
                w.writeValue(typeName.getNamespaceURI() == null ? "" : typeName.getNamespaceURI());

                w.writeStartAttribute("", "name");
                w.writeValue(typeName.getLocalPart());

                String binding = attributeType.getBinding().getName();
                w.writeStartAttribute("", "binding");
                w.writeValue(binding);

                if (attributeType instanceof GeometryType) {
                    GeometryType gt = (GeometryType) attributeType;
                    CoordinateReferenceSystem crs = gt.getCoordinateReferenceSystem();
                    String srsName;
                    if (crs == null) {
                        srsName = "urn:ogc:def:crs:EPSG::0";
                    } else {
                        srsName = CRS.toSRS(crs);
                    }
                    if (srsName != null) {
                        w.writeStartAttribute("", "srsName");
                        w.writeValue(srsName);
                    } else {
                        String wkt;
                        if (crs instanceof Formattable) {
                            wkt = ((Formattable) crs).toWKT(Formattable.SINGLE_LINE);
                        } else {
                            wkt = crs.toWKT();
                        }
                        w.writeStartAttribute("", "srsWKT");
                        w.writeValue(wkt);
                    }
                }

                w.writeEndAttributes();
                w.writeEndElement();
            }
            // Map<Object, Object> userData = descriptor.getUserData();

            w.writeEndElement();

        }
        w.writeEndElement();
        w.writeEndDocument();
    }
}
