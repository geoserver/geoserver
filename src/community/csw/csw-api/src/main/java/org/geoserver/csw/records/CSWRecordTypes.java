/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.geotools.csw.CSW;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.feature.NameImpl;
import org.geotools.feature.TypeBuilder;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.geotools.filter.v1_0.OGC;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.OWS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.ComplexType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.xml.sax.helpers.NamespaceSupport;

import com.vividsolutions.jts.geom.MultiPolygon;

/**
 * Container for FeatureType related constants useful in building CSW records
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CSWRecordTypes {

    /**
     * Contains the declarations of common namespaces and prefixes used in the CSW world
     */
    public static final NamespaceSupport NAMESPACES;

    public static final ComplexType SIMPLE_LITERAL;

    public static final Name SIMPLE_LITERAL_SCHEME;

    public static final Name SIMPLE_LITERAL_VALUE;

    public static final Map<String, AttributeDescriptor> DC_DESCRIPTORS;

    public static final Map<String, AttributeDescriptor> DCT_DESCRIPTORS;

    public static final AttributeDescriptor DC_ELEMENT;

    public static final Name DC_ELEMENT_NAME;

    public static final NameImpl RECORD_BBOX_NAME;

    public static final AttributeDescriptor RECORD_BBOX_DESCRIPTOR;
    
    public static final NameImpl RECORD_GEOMETRY_NAME;

    public static final AttributeDescriptor RECORD_GEOMETRY_DESCRIPTOR;


    public static final FeatureType RECORD;

    static {
        // prepare the common namespace support
        NAMESPACES = new NamespaceSupport();
        NAMESPACES.declarePrefix("csw", CSW.NAMESPACE);
        NAMESPACES.declarePrefix("rim", "urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0");
        NAMESPACES.declarePrefix("dc", DC.NAMESPACE);
        NAMESPACES.declarePrefix("dct", DCT.NAMESPACE);
        NAMESPACES.declarePrefix("ows", OWS.NAMESPACE);
        NAMESPACES.declarePrefix("ogc", OGC.NAMESPACE);
        
        // prepare the CSW record related types
        FeatureTypeFactory typeFactory = new FeatureTypeFactoryImpl();
        TypeBuilder builder = new TypeBuilder(typeFactory);

        try {
            // create the SimpleLiteral type
            builder.setNamespaceURI(DC.SimpleLiteral.getNamespaceURI());
            builder.name("scheme");
            builder.bind(URI.class);
            AttributeType schemeType = builder.attribute();
            builder.setNamespaceURI(DC.SimpleLiteral.getNamespaceURI());
            builder.name("value");
            builder.bind(Object.class);
            AttributeType valueType = builder.attribute();
            builder.setNillable(true);
            builder.addAttribute("scheme", schemeType);
            builder.addAttribute("value", valueType);
            builder.setName("SimpleLiteral");
            SIMPLE_LITERAL = builder.complex();
            SIMPLE_LITERAL_SCHEME = new NameImpl(DC.NAMESPACE, "scheme");
            SIMPLE_LITERAL_VALUE = new NameImpl(DC.NAMESPACE, "value");

            // create the DC_ELEMENT
            builder.setNamespaceURI(DC.NAMESPACE);
            builder.setName(DC.DCelement.getLocalPart());
            builder.setPropertyType(SIMPLE_LITERAL);
            builder.setMinOccurs(0);
            builder.setMaxOccurs(-1);
            DC_ELEMENT = builder.attributeDescriptor();
            DC_ELEMENT_NAME = new NameImpl(DC.NAMESPACE, DC.DCelement.getLocalPart());

            // fill in the DC attribute descriptors
            DC_DESCRIPTORS = new HashMap<String, AttributeDescriptor>();
            fillSimpleLiteralDescriptors(builder, DC.class, DC_DESCRIPTORS,
                    Arrays.asList("DC-Element", "elementContainer", "SimpleLiteral"));

            // fill in the DCT attribute descriptors
            DCT_DESCRIPTORS = new HashMap<String, AttributeDescriptor>();
            fillSimpleLiteralDescriptors(builder, DCT.class, DCT_DESCRIPTORS,
                    new ArrayList<String>());

            // create the geometry representation, used for the sake of in memory filtering 
            // and spatial representation in a single CRS
            builder.setNamespaceURI(CSW.NAMESPACE);
            builder.setName("geometry");
            builder.setBinding(MultiPolygon.class);
            builder.crs(DefaultGeographicCRS.WGS84);
            GeometryType geometryType = builder.geometry();
            builder.setMinOccurs(0);
            builder.setMaxOccurs(1);
            builder.setNamespaceURI(CSW.NAMESPACE);
            builder.setName("geometry");
            builder.setPropertyType(geometryType);
            RECORD_GEOMETRY_DESCRIPTOR = builder.attributeDescriptor();
            RECORD_GEOMETRY_NAME = new NameImpl(CSW.NAMESPACE, "geometry");
            builder.setDefaultGeometry(RECORD_GEOMETRY_NAME);
            
            // and now the actual bbox, as a ReferencedEnvelope with the native CRS
            builder.setNamespaceURI(CSW.NAMESPACE);
            builder.setName("BoundingBoxType");
            builder.setBinding(ReferencedEnvelope.class);
            AttributeType bboxType = builder.attribute();
            builder.setMinOccurs(0);
            builder.setMaxOccurs(-1);
            builder.setNamespaceURI(CSW.NAMESPACE);
            builder.setName("BoundingBox");
            builder.setPropertyType(bboxType);
            RECORD_BBOX_DESCRIPTOR = builder.attributeDescriptor();
            RECORD_BBOX_NAME = new NameImpl(CSW.NAMESPACE, "BoundingBox");

            // create the CSW record
            builder.setNamespaceURI(CSW.NAMESPACE);
            builder.setName(CSW.Record.getLocalPart());
            builder.add(DC_ELEMENT);
            builder.add(RECORD_BBOX_DESCRIPTOR);
            builder.add(RECORD_GEOMETRY_DESCRIPTOR);
            RECORD = builder.feature();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to create one of the attribute descriptors for "
                    + "the DC or DCT elements", e);
        }
    }

    /**
     * Locates the AttributeDescriptor corresponding to the specified element name among the ones
     * available in {@link CSWRecordTypes#DC_DESCRIPTORS} and {@link CSWRecordTypes#DCT_DESCRIPTORS} 
     * 
     * @param elementName
     * @return
     */
    public static AttributeDescriptor getDescriptor(String elementName) {
        AttributeDescriptor identifierDescriptor = CSWRecordTypes.DC_DESCRIPTORS.get(elementName);
        if (identifierDescriptor == null) {
            identifierDescriptor = CSWRecordTypes.DCT_DESCRIPTORS.get(elementName);
        }
        return identifierDescriptor;
    }

    private static void fillSimpleLiteralDescriptors(TypeBuilder builder, Class schemaClass,
            Map<String, AttributeDescriptor> descriptors, List<String> blacklist)
            throws IllegalAccessException {
        for (Field field : schemaClass.getFields()) {
            if (isConstant(field) && QName.class.equals(field.getType())) {
                QName name = (QName) field.get(null);
                String localName = name.getLocalPart();
                if (!blacklist.contains(localName)) {
                    // build the descriptor
                    builder.setPropertyType(SIMPLE_LITERAL);
                    builder.setNamespaceURI(name.getNamespaceURI());
                    builder.setName(localName);
                    AttributeDescriptor descriptor = builder.attributeDescriptor();
                    descriptors.put(localName, descriptor);
                }
            }
        }
    }

    /**
     * Checks if a field is public static final
     * 
     * @param field
     * @return
     */
    static boolean isConstant(Field field) {
        int modifier = field.getModifiers();
        return Modifier.isStatic(modifier) && Modifier.isPublic(modifier)
                && Modifier.isFinal(modifier);
    }

}
