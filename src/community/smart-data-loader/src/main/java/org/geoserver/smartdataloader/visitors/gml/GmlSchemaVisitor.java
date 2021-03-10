/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.visitors.gml;

import static org.geoserver.smartdataloader.visitors.gml.GmlSchemaUtils.buildGmlSchemaDocument;
import static org.geoserver.smartdataloader.visitors.gml.GmlSchemaUtils.createComplexAttributeElementNode;
import static org.geoserver.smartdataloader.visitors.gml.GmlSchemaUtils.createFeatureElementNode;
import static org.geoserver.smartdataloader.visitors.gml.GmlSchemaUtils.createFeatureTypeNode;
import static org.geoserver.smartdataloader.visitors.gml.GmlSchemaUtils.createPropertyTypeNode;
import static org.geoserver.smartdataloader.visitors.gml.GmlSchemaUtils.createSimpleAttributeElementNode;
import static org.geoserver.smartdataloader.visitors.gml.GmlSchemaUtils.getFeatureElementNodeByName;

import org.geoserver.smartdataloader.domain.DomainModelVisitorImpl;
import org.geoserver.smartdataloader.domain.entities.DomainEntity;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;
import org.geoserver.smartdataloader.domain.entities.DomainRelation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** This visitor will generate a valid GML 3.2 schema for the domain model it will visit. */
public final class GmlSchemaVisitor extends DomainModelVisitorImpl {

    private final String targetNamespacePrefix;

    private final Document gmlDocument;

    private Element currentComplexType;

    public GmlSchemaVisitor(String targetNamespacePrefix, String targetNamespaceUrl) {
        this.targetNamespacePrefix = targetNamespacePrefix;
        // build and instantiate the gml schema document
        gmlDocument = buildGmlSchemaDocument(targetNamespacePrefix, targetNamespaceUrl);
    }

    @Override
    public void visitDomainRootEntity(DomainEntity entity) {
        // we don't need a property type, because this feature is never chained
        handleEntity(entity, false);
    }

    @Override
    public void visitDomainChainedEntity(DomainEntity entity) {
        // we need a property type since this entity is chained at some point
        handleEntity(entity, true);
    }

    @Override
    public void visitDomainRelation(DomainRelation relation) {
        // let's check that we are in a valid state
        if (currentComplexType == null) {
            // no complex type being encoded, so there is nothing we can do
            throw new RuntimeException(
                    "There is no current complex type being encoded for the complex attribute '"
                            + relation.getDestinationEntity().getName()
                            + "'.");
        }
        Node featureTypeNode =
                getFeatureElementNodeByName(
                        gmlDocument, relation.getContainingEntity().getGmlInfo().complexTypeName());
        Element sequenceNode =
                (Element) featureTypeNode.getFirstChild().getFirstChild().getFirstChild();
        // let's proceed with the complex attribute encoding
        Element attributeElement =
                createComplexAttributeElementNode(
                        gmlDocument, relation.getDestinationEntity(), targetNamespacePrefix);
        // retrieve the sequence node of the current complex type
        sequenceNode.appendChild(attributeElement);
    }

    @Override
    public void visitDomainEntitySimpleAttribute(DomainEntitySimpleAttribute attribute) {
        // let's check that we are in a valid state
        if (currentComplexType == null) {
            // no complex type being encoded, so there is nothing we can do
            throw new RuntimeException(
                    "There is no current complex type being encoded for the simple attribute '"
                            + attribute.getName()
                            + "'.");
        }
        // let's proceed with the simple attribute encoding
        Element attributeElement = createSimpleAttributeElementNode(gmlDocument, attribute);
        // retrieve the sequence node of the current complex type
        Element sequenceNode =
                (Element) currentComplexType.getFirstChild().getFirstChild().getFirstChild();
        sequenceNode.appendChild(attributeElement);
    }

    /**
     * Helper method that creates the necessary declarations for a domain entity, attributes
     * elements are not created. The creation of a property type is optional, since it's only needed
     * for feature types that will be feature chained.
     */
    private void handleEntity(DomainEntity entity, boolean createPropertyType) {
        // create the complex type node for the entity feature type
        Element complexType = createFeatureTypeNode(gmlDocument, entity);
        gmlDocument.getFirstChild().appendChild(complexType);
        // create the element declaration node for the entity feature type
        Element elementDeclaration =
                createFeatureElementNode(gmlDocument, entity, targetNamespacePrefix);
        gmlDocument.getFirstChild().appendChild(elementDeclaration);
        // create the correspondent property type, used for feature chaining, if needed
        if (createPropertyType) {
            Element propertyType =
                    createPropertyTypeNode(gmlDocument, entity, targetNamespacePrefix);
            gmlDocument.getFirstChild().appendChild(propertyType);
        }
        // set the entity complex type as the current one being encoded
        currentComplexType = complexType;
    }

    public Document getDocument() {
        return gmlDocument;
    }
}
