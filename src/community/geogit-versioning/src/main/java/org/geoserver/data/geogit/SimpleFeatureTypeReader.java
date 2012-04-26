package org.geoserver.data.geogit;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.geogit.api.ObjectId;
import org.geogit.storage.ObjectReader;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.gvsig.bxml.stream.BxmlFactoryFinder;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.stream.EventType;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class SimpleFeatureTypeReader implements ObjectReader<SimpleFeatureType> {

    private final Name typeName;

    public SimpleFeatureTypeReader(final Name typeName) {
        Preconditions.checkNotNull(typeName);
        this.typeName = typeName;
    }

    @Override
    public SimpleFeatureType read(final ObjectId id, final InputStream rawData) throws IOException,
            IllegalArgumentException {

        final BxmlStreamReader reader = BxmlFactoryFinder.newInputFactory().createScanner(rawData);
        try {
            reader.nextTag();
            reader.require(EventType.START_ELEMENT, "", "type");
            final String _class = reader.getAttributeValue("", "class");
            Preconditions.checkArgument("SimpleFeatureType".equals(_class), "Wrong type class: "
                    + _class + ". Expected 'SimpleFeatureType'");

            SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
            builder.setName(typeName);

            try {
                parseDescriptors(reader, builder);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                Throwables.propagate(e);
            }

            SimpleFeatureType type = builder.buildFeatureType();
            return type;
        } finally {
            reader.close();
        }
    }

    private List<AttributeDescriptor> parseDescriptors(final BxmlStreamReader r,
            final SimpleFeatureTypeBuilder builder) throws Exception {

        final FeatureTypeFactory typeFactory = builder.getFeatureTypeFactory();

        r.require(EventType.START_ELEMENT, "", "type");
        EventType event;
        while ((event = r.nextTag()) != EventType.END_DOCUMENT) {
            if (EventType.END_ELEMENT.equals(event)
                    && "type".equals(r.getElementName().getLocalPart())) {
                break;
            }
            r.require(EventType.START_ELEMENT, "", "attribute");

            final boolean nillable = Boolean.parseBoolean(r.getAttributeValue(null, "nillable"));
            final String namespace = r.getAttributeValue(null, "namespace");
            final String localName = r.getAttributeValue(null, "name");

            final int maxOccurs = Integer.parseInt(r.getAttributeValue(null, "maxOccurs"));
            final int minOccurs = Integer.parseInt(r.getAttributeValue(null, "minOccurs"));
            final AttributeType attributeType;

            r.nextTag();
            r.require(EventType.START_ELEMENT, null, "type");
            attributeType = parseAttributeType(r, typeFactory);
            r.require(EventType.END_ELEMENT, null, "type");

            r.nextTag();
            r.require(EventType.END_ELEMENT, "", "attribute");

            Object defaultValue = null;
            final Name attributeName;
            if (namespace == null || namespace.length() == 0) {
                attributeName = new NameImpl(localName);
            } else {
                attributeName = new NameImpl(namespace, localName);
            }

            final AttributeDescriptor descriptor;
            if (attributeType instanceof GeometryType) {
                descriptor = typeFactory.createGeometryDescriptor((GeometryType) attributeType,
                        attributeName, minOccurs, maxOccurs, nillable, defaultValue);
            } else {
                descriptor = typeFactory.createAttributeDescriptor(attributeType, attributeName,
                        minOccurs, maxOccurs, nillable, defaultValue);
            }

            builder.add(descriptor);
        }

        r.require(EventType.END_ELEMENT, "", "type");
        return null;
    }

    private AttributeType parseAttributeType(final BxmlStreamReader r,
            final FeatureTypeFactory typeFactory) throws Exception {

        r.require(EventType.START_ELEMENT, null, "type");

        final String typeNs = r.getAttributeValue(null, "namespace");
        final String typeLocalName = r.getAttributeValue(null, "name");
        Class<?> binding;
        try {
            binding = Class.forName(r.getAttributeValue(null, "binding"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        final String srsName = r.getAttributeValue(null, "srsName");
        final String srsWKT = r.getAttributeValue(null, "srsWKT");

        r.nextTag();
        r.require(EventType.END_ELEMENT, null, "type");

        final boolean isIdentifiable = false;
        boolean isAbstract = false;
        final List<Filter> restrictions = null;
        final AttributeType superType = null;
        final InternationalString description = null;
        Name attributeTypeName;
        if (typeNs == null || typeNs.length() == 0) {
            attributeTypeName = new NameImpl(typeLocalName);
        } else {
            attributeTypeName = new NameImpl(typeNs, typeLocalName);
        }
        AttributeType attributeType;
        if (srsName == null && srsWKT == null) {
            attributeType = typeFactory.createAttributeType(attributeTypeName, binding,
                    isIdentifiable, isAbstract, restrictions, superType, description);
        } else {
            CoordinateReferenceSystem crs;
            if (srsName != null) {
                if ("urn:ogc:def:crs:EPSG::0".equals(srsName)) {
                    crs = null;
                } else {
                    crs = CRS.decode(srsName);
                }
            } else {
                crs = CRS.parseWKT(srsWKT);
            }
            attributeType = typeFactory.createGeometryType(attributeTypeName, binding, crs,
                    isIdentifiable, isAbstract, restrictions, superType, description);
        }

        return attributeType;
    }

}
