/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.visitors.gml;

import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geoserver.smartdataloader.domain.entities.DomainAttributeType;
import org.geoserver.smartdataloader.domain.entities.DomainEntity;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** This class contains helper methods useful to generate a GML schema for a domain entity. */
public final class GmlSchemaUtils {

    private static final Map<DomainAttributeType, String> DOMAIN_TYPES_MAPPING = new HashMap<>();

    static {
        DOMAIN_TYPES_MAPPING.put(DomainAttributeType.TEXT, "xs:string");
        DOMAIN_TYPES_MAPPING.put(DomainAttributeType.INT, "xs:int");
        DOMAIN_TYPES_MAPPING.put(DomainAttributeType.INTEGER, "xs:integer");
        DOMAIN_TYPES_MAPPING.put(DomainAttributeType.NUMBER, "xs:double");
        DOMAIN_TYPES_MAPPING.put(DomainAttributeType.DATE, "xs:dateTime");
        DOMAIN_TYPES_MAPPING.put(DomainAttributeType.BOOLEAN, "xs:boolean");
        DOMAIN_TYPES_MAPPING.put(DomainAttributeType.GEOMETRY, "gml:GeometryPropertyType");
    }

    private GmlSchemaUtils() {}

    /** Helper method that creates a new empty GML schema document. */
    static Document buildGmlSchemaDocument(
            String targetNamespacePrefix, String targetNamespaceUrl) {
        // build the gml schema document
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = docBuilder.newDocument();
            return initiateGmlSchemaDocument(document, targetNamespacePrefix, targetNamespaceUrl);
        } catch (Exception exception) {
            throw new RuntimeException("Error build GML document.", exception);
        }
    }

    /** Helper method that will initiate the provided XML document as a GML schema. */
    static Document initiateGmlSchemaDocument(
            Document document, String targetNamespacePrefix, String targetNamespaceUrl) {
        // add the schema metadata node
        Element metadataNode = document.createElement("xs:schema");
        metadataNode.setAttribute("version", "1.0");
        metadataNode.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
        metadataNode.setAttribute("xmlns:gml", "http://www.opengis.net/gml/3.2");
        metadataNode.setAttribute("xmlns:" + targetNamespacePrefix, targetNamespaceUrl);
        metadataNode.setAttribute("targetNamespace", targetNamespaceUrl);
        metadataNode.setAttribute("elementFormDefault", "qualified");
        metadataNode.setAttribute("attributeFormDefault", "unqualified");
        document.appendChild(metadataNode);
        // add the import of the gml 3.2 schema
        Element importGmlNode = document.createElement("xs:import");
        importGmlNode.setAttribute("namespace", "http://www.opengis.net/gml/3.2");
        importGmlNode.setAttribute(
                "schemaLocation", "http://schemas.opengis.net/gml/3.2.1/gml.xsd");
        metadataNode.appendChild(importGmlNode);
        // return the initiated gml schema document
        return document;
    }

    /**
     * Helper method that creates a GML complex type declaration node for the provided domain entity. The
     * created node is not attached to the GML document and doesn't contain any attribute.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <xs:complexTypeNode name="<DOMAIN_ENTITY_NAME>Type">
     *   <xs:complexContentNode>
     *     <xs:extensionNode base="gml:AbstractFeatureType">
     *       <xs:sequenceNode>
     *       (...)
     *       </xs:sequenceNode>
     *     </xs:extensionNode>
     *   </xs:complexContentNode>
     * </xs:complexTypeNode>}
     */
    static Element createFeatureTypeNode(Document gmlSchema, DomainEntity entity) {
        // create complex type node
        Element complexTypeNode = gmlSchema.createElement("xs:complexType");
        complexTypeNode.setAttribute("name", entity.getGmlInfo().complexTypeName());
        // create the content node
        Element complexContentNode = gmlSchema.createElement("xs:complexContent");
        complexTypeNode.appendChild(complexContentNode);
        // create the extension node
        Element extensionNode = gmlSchema.createElement("xs:extension");
        extensionNode.setAttribute("base", "gml:AbstractFeatureType");
        complexContentNode.appendChild(extensionNode);
        // create the sequence node
        Element sequenceNode = gmlSchema.createElement("xs:sequence");
        extensionNode.appendChild(sequenceNode);
        // we are done, return the created element detached from the document
        return complexTypeNode;
    }

    /**
     * Helper method that creates a GML property type for a feature type, used for feature chaining. The
     * created node is not attached to the GML document.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <xs:complexType name="<DOMAIN_ENTITY_NAME>PropertyType">
     *   <xs:sequence minOccurs="0">
     *     <xs:element ref="<TARGET_NAMESPACE_PREFIX>:<DOMAIN_ENTITY_NAME>" />
     *   </xs:sequence>
     *   <xs:attributeGroup ref="gml:AssociationAttributeGroup" />
     * </xs:complexType>}
     */
    static Element createPropertyTypeNode(
            Document gmlSchema, DomainEntity entity, String targetNamespacePrefix) {
        // create complex type node
        Element complexTypeNode = gmlSchema.createElement("xs:complexType");
        complexTypeNode.setAttribute("name", entity.getGmlInfo().complexPropertyTypeName());
        // create the sequence node
        Element sequenceNode = gmlSchema.createElement("xs:sequence");
        sequenceNode.setAttribute("minOccurs", "0");
        complexTypeNode.appendChild(sequenceNode);
        // create the element node
        Element elementNode = gmlSchema.createElement("xs:element");
        elementNode.setAttribute(
                "ref", targetNamespacePrefix + ":" + entity.getGmlInfo().featureTypeName());
        sequenceNode.appendChild(elementNode);
        // create the attribute group node
        Element attributeGroupNode = gmlSchema.createElement("xs:attributeGroup");
        attributeGroupNode.setAttribute("ref", "gml:AssociationAttributeGroup");
        complexTypeNode.appendChild(attributeGroupNode);
        // we are done, return the created element detached from the document
        return complexTypeNode;
    }

    /**
     * Helper method that creates an element declaration for a feature type. The created node is not attached to the
     * GML document.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code <xs:element name="Parameter" type="st:ParameterType" substitutionGroup="gml:AbstractFeature"/>}
     */
    static Element createFeatureElementNode(
            Document gmlSchema, DomainEntity entity, String targetNamespacePrefix) {
        // create element node
        Element elementNode = gmlSchema.createElement("xs:element");
        elementNode.setAttribute("name", entity.getGmlInfo().featureTypeName());
        elementNode.setAttribute(
                "type", targetNamespacePrefix + ":" + entity.getGmlInfo().complexTypeName());
        elementNode.setAttribute("substitutionGroup", "gml:AbstractFeature");
        // we are done, return the created element detached from the document
        return elementNode;
    }

    /**
     * Helper method that creates an element declaration for an entity simple attribute type. The created node is not attached to the
     * GML document.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code <xs:element name="<ATTRIBUTE_NAME>" minOccurs="0" maxOccurs="1" type="<ATTRIBUTE_TYPE>"/>}
     */
    static Element createSimpleAttributeElementNode(
            Document gmlSchema, DomainEntitySimpleAttribute attribute) {
        // create element node
        Element elementNode = gmlSchema.createElement("xs:element");
        elementNode.setAttribute("name", attribute.getName());
        elementNode.setAttribute("minOccurs", "0");
        elementNode.setAttribute("maxOccurs", "1");
        elementNode.setAttribute("type", DOMAIN_TYPES_MAPPING.get(attribute.getType()));
        // we are done, return the created element detached from the document
        return elementNode;
    }

    /**
     * Helper method that creates an element declaration for an entity complex attribute type. The created node is not attached to the
     * GML document.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code <xs:element name="<DESTINATION_ENTITY_NAME>" minOccurs="0" maxOccurs="unbounded" type="DESTINATION_ENTITY_PROPERTY_TYPE" />"/>}
     */
    static Element createComplexAttributeElementNode(
            Document gmlSchema, DomainEntity destinationEntity, String targetNamespacePrefix) {
        // create element node
        Element elementNode = gmlSchema.createElement("xs:element");
        elementNode.setAttribute("name", destinationEntity.getGmlInfo().complexTypeAttributeName());
        elementNode.setAttribute("minOccurs", "0");
        elementNode.setAttribute("maxOccurs", "unbounded");
        elementNode.setAttribute(
                "type",
                targetNamespacePrefix
                        + ":"
                        + destinationEntity.getGmlInfo().complexPropertyTypeName());
        // we are done, return the created element detached from the document
        return elementNode;
    }

    /**
     * Helper method that finds a complex type declaration by name on the provided GML document.
     * NULL is returned if no matching complex type declaration is found.
     */
    static Node getFeatureElementNodeByName(Document gmlSchema, String complexTypeName) {
        NodeList complexTypes = gmlSchema.getElementsByTagName("xs:complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Node candidateNode = complexTypes.item(i);
            String candidateName =
                    candidateNode.getAttributes().getNamedItem("name").getNodeValue();
            if (candidateName.equals(complexTypeName)) {
                // we found our complex type
                return candidateNode;
            }
        }
        // no type found
        return null;
    }
}
