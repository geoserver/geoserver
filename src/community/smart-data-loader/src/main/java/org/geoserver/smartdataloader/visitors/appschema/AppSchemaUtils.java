/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.visitors.appschema;

import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** This class contains helper methods useful to generate a AppSchema xml document. */
public final class AppSchemaUtils {

    private AppSchemaUtils() {}

    /** Helper method that creates a new empty AppSchema document. */
    static Document buildAppSchemaDocument(
            String targetNamespacePrefix, String targetNamespaceUrl) {
        // build the appschema document
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = docBuilder.newDocument();
            return initiateAppSchemaDocument(document, targetNamespacePrefix, targetNamespaceUrl);
        } catch (Exception exception) {
            throw new RuntimeException("Error build AppSchema document.", exception);
        }
    }

    /** Helper method that will initiate the provided XML document as an Appschema. */
    static Document initiateAppSchemaDocument(
            Document document, String targetNamespacePrefix, String targetNamespaceUrl) {
        // root node
        Element metadataNode = document.createElement("ns3:AppSchemaDataAccess");
        metadataNode.setAttribute("xmlns:ns2", "http://www.opengis.net/ogc");
        metadataNode.setAttribute("xmlns:ns3", "http://www.geotools.org/app-schema");
        document.appendChild(metadataNode);
        // namespaces
        Element namespacesNode = document.createElement("namespaces");
        Node gmlNamespace = createNamespaceNode(document, "gml", "http://www.opengis.net/gml/3.2");
        namespacesNode.appendChild(gmlNamespace);
        Node localNamespace =
                createNamespaceNode(document, targetNamespacePrefix, targetNamespaceUrl);
        namespacesNode.appendChild(localNamespace);
        metadataNode.appendChild(namespacesNode);
        // empty includedTypes node
        Element includedTypesNode = document.createElement("includedTypes");
        metadataNode.appendChild(includedTypesNode);
        return document;
    }

    /**
     * Helper method that allows to get the sourceDataStores node from a given document. Method is
     * assuming that there is only one element with sourceDataStores tagname.
     */
    static Node getSourceDataStoresNode(Document document) {
        NodeList nodes = document.getElementsByTagName("sourceDataStores");
        if (nodes != null && nodes.getLength() > 0) return nodes.item(0);
        return null;
    }

    /**
     * Helper method that allows to get the typeMappings node from a given document. Method is
     * assuming that there is only one element with typeMappings tagname.
     */
    static Node getTypeMappingsNode(Document document) {
        NodeList nodes = document.getElementsByTagName("typeMappings");
        if (nodes != null && nodes.getLength() > 0) return nodes.item(0);
        return null;
    }

    /**
     * Helper method that creates the sourceDataStores node.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <sourceDataStores>
     *  (...)
     * </sourceDataStores>}
     */
    static Element createSourceDataStoresNode(Document document) {
        Element sourceDataStoresNode = document.createElement("sourceDataStores");
        return sourceDataStoresNode;
    }

    /**
     * Helper method that creates the typeMappings node.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <typeMappings>
     *  (...)
     * </typeMappings>}
     */
    static Element createTypeMappingsNode(Document document) {
        Element typeMappingsNode = document.createElement("typeMappings");
        return typeMappingsNode;
    }

    /**
     * Helper method that creates the targetTypes node.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <targetTypes>
     *  (...)
     * </targetTypes>}
     */
    static Element createTargetTypesNode(Document document, String schemaUri) {
        Element targetTypesNode = document.createElement("targetTypes");
        Node featureTypeNode = createFeatureTypeNode(document, schemaUri);
        targetTypesNode.appendChild(featureTypeNode);
        return targetTypesNode;
    }

    /**
     * Helper method that creates a Namespace node (which will be included in namespaces tagname).
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <Namespace>
     *     <prefix>PREFIX_VALUE</prefix>
     *     <uri>URI_VALUE</uri>
     * </Namespace>}
     */
    static Node createNamespaceNode(Document document, String prefixValue, String uriValue) {
        Element namespaceNode = document.createElement("Namespace");
        Element prefixNamespaceNode = document.createElement("prefix");
        prefixNamespaceNode.setTextContent(prefixValue);
        Element uriNamespaceNode = document.createElement("uri");
        uriNamespaceNode.setTextContent(uriValue);
        namespaceNode.appendChild(prefixNamespaceNode);
        namespaceNode.appendChild(uriNamespaceNode);
        return namespaceNode;
    }

    /**
     * Helper method that creates a Parameter node (which will be included in parameters list for a DataStore definition).
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <Parameter>
     *     <name>PARAMETER_NAME</name>
     *     <value>PARAMETER_VALUE</value>
     * </Parameter>}
     */
    static Node createParameterNode(
            Document document, String parameterName, String parameterValue) {
        Element parameterNode = document.createElement("Parameter");
        Element nameParameterNode = document.createElement("name");
        nameParameterNode.setTextContent(parameterName);
        Element valueParameterNode = document.createElement("value");
        valueParameterNode.setTextContent(parameterValue);
        parameterNode.appendChild(nameParameterNode);
        parameterNode.appendChild(valueParameterNode);
        return parameterNode;
    }

    /**
     * Helper method that creates a DataStore node for a given DataStoreMetadata.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <DataStore>
     *		<id>DATASTORE_ID</id>
     *		<parameters>
     *			<Parameter>
     *				(...)
     * 			</Parameter>
     *			(...)
     *		</parameters>
     * </DataStore>}
     */
    static Node createDataStoreNode(Document document, DataStoreMetadata dataStoreMetadata) {
        Element dataStoreNode = document.createElement("DataStore");
        Element idNode = document.createElement("id");
        idNode.setTextContent(dataStoreMetadata.getName());
        dataStoreNode.appendChild(idNode);
        Element parametersNode = document.createElement("parameters");
        Map<String, String> params = dataStoreMetadata.getDataStoreMetadataConfig().getParameters();
        Set<String> keys = params.keySet();
        keys.forEach(
                key -> {
                    String value = params.get(key);
                    Node parameterNode = createParameterNode(document, key, value);
                    parametersNode.appendChild(parameterNode);
                });
        dataStoreNode.appendChild(parametersNode);
        return dataStoreNode;
    }

    /**
     * Helper method that creates the FeatureType node (which will be included into targetTypes node).
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <FeatureType>
     *     <schemaUri>SCHEMA_URI</schemaUri>
     * </FeatureType>}
     */
    static Node createFeatureTypeNode(Document document, String schemaUriValue) {
        Element featureTypeNode = document.createElement("FeatureType");
        Element schemaUriNode = document.createElement("schemaUri");
        schemaUriNode.setTextContent(schemaUriValue);
        featureTypeNode.appendChild(schemaUriNode);
        return featureTypeNode;
    }

    /**
     * Helper method that creates a FeatureTypeMapping node.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <FeatureTypeMapping>
     * 		<sourceDataStore>DATASTORE_ID</sourceDataStore>
     *		<sourceType>SOURCE_TYPE</sourceType>
     *		<targetElement>TARGET_ELEMENT_NAME</targetElement>
     *		<attributeMappings>
     *			<AttributeMapping>
     *				(...)
     *   		</AttributeMapping>
     *   		( ... )
     * 		</attributeMappings>
     * </FeatureTypeMapping>}
     */
    static Element createFeatureTypeMappingNode(
            Document document,
            String sourceDataStoreValue,
            String sourceTypeValue,
            String targetElementValue) {
        Element featureTypeMappingNode = document.createElement("FeatureTypeMapping");
        Element sourceDataStoreNode = document.createElement("sourceDataStore");
        sourceDataStoreNode.setTextContent(sourceDataStoreValue);
        featureTypeMappingNode.appendChild(sourceDataStoreNode);
        Element sourceTypeNode = document.createElement("sourceType");
        sourceTypeNode.setTextContent(sourceTypeValue);
        featureTypeMappingNode.appendChild(sourceTypeNode);
        Element targetElementNode = document.createElement("targetElement");
        targetElementNode.setTextContent(targetElementValue);
        featureTypeMappingNode.appendChild(targetElementNode);
        Element attributeMappingsNode = document.createElement("attributeMappings");
        featureTypeMappingNode.appendChild(attributeMappingsNode);
        return featureTypeMappingNode;
    }

    /**
     * Helper method that creates an AttributeMapping node that is linked to another FeatureTypeMapping.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <AttributeMapping>
     *     <isMutiple>true</isMutiple>
     *     <targetAttribute>TARGET_ATTRIBUTE_NAME</targetAttribute>
     *     <sourceExpression>
     *       <linkField>LINKED_FIELD_NAME</linkField>
     *       <linkElement>LINKED_FEATURETYPEMAPPING</linkElement>
     *       <OCQL>OCQL_VALUE</OCQL>
     *     </sourceExpression>
     *   </AttributeMapping>}
     */
    static Node createLinkedAttributeMapping(
            Document document,
            String targetAttributeValue,
            String OCQLValue,
            String linkedElement,
            String linkedField) {
        Element attributeMappingNode = document.createElement("AttributeMapping");
        Element targetAttributeNode = document.createElement("targetAttribute");
        targetAttributeNode.setTextContent(targetAttributeValue);
        Element sourceExpressionNode = document.createElement("sourceExpression");
        Element OCQLNode = document.createElement("OCQL");
        OCQLNode.setTextContent(OCQLValue);
        Element linkElementNode = document.createElement("linkElement");
        linkElementNode.setTextContent(linkedElement);
        Element linkFieldNode = document.createElement("linkField");
        linkFieldNode.setTextContent(linkedField);
        sourceExpressionNode.appendChild(linkFieldNode);
        sourceExpressionNode.appendChild(linkElementNode);
        sourceExpressionNode.appendChild(OCQLNode);
        Element isMultiple = document.createElement("isMutiple");
        isMultiple.setTextContent("true");
        // attributeMappingNode.appendChild(isMultiple);
        attributeMappingNode.appendChild(targetAttributeNode);
        attributeMappingNode.appendChild(sourceExpressionNode);

        return attributeMappingNode;
    }

    /**
     * Helper method that creates an AttributeMapping node.
     *
     * The created node structure will look like this:
     *
     * <pre>{@code
     * <AttributeMapping>
     *    <targetAttribute>ATTRIBUTE_NAME</targetAttribute>
     *     <sourceExpression>
     *       <OCQL>OCQL_VALUE</OCQL>
     *     </sourceExpression>
     * </AttributeMapping>}
     */
    static Node createAttributeMapping(
            Document document, String targetAttributeValue, String OCQLValue) {
        Element attributeMappingNode = document.createElement("AttributeMapping");
        Element targetAttributeNode = document.createElement("targetAttribute");
        targetAttributeNode.setTextContent(targetAttributeValue);
        Element sourceExpressionNode = document.createElement("sourceExpression");
        Element OCQLNode = document.createElement("OCQL");
        OCQLNode.setTextContent(OCQLValue);
        sourceExpressionNode.appendChild(OCQLNode);
        attributeMappingNode.appendChild(targetAttributeNode);
        attributeMappingNode.appendChild(sourceExpressionNode);
        return attributeMappingNode;
    }

    static Node createAttributeMappingIdExpression(
            Document document, String typeMappingTarget, String OCQLValue) {
        int sep = typeMappingTarget.indexOf(":");
        String idPrefix = typeMappingTarget;
        if (sep != -1) idPrefix = typeMappingTarget.substring(sep + 1);
        Element attributeMappingNode = document.createElement("AttributeMapping");
        Element targetAttributeNode = document.createElement("targetAttribute");
        targetAttributeNode.setTextContent(typeMappingTarget);
        Element sourceExpressionNode = document.createElement("idExpression");
        Element OCQLNode = document.createElement("OCQL");
        OCQLValue = "strConcat('" + idPrefix + ".'," + OCQLValue + ")";
        OCQLNode.setTextContent(OCQLValue);
        sourceExpressionNode.appendChild(OCQLNode);
        attributeMappingNode.appendChild(targetAttributeNode);
        attributeMappingNode.appendChild(sourceExpressionNode);
        return attributeMappingNode;
    }

    static void updateAttributeMappingIdExpression(Node idExpression, String OCQLValue) {
        Node ocqlIdExpr = idExpression.getFirstChild();
        String strIdExpr = ocqlIdExpr.getTextContent();
        strIdExpr = "strConcat(strConcat(" + strIdExpr + "," + "'.')," + OCQLValue + ")";
        ocqlIdExpr.setTextContent(strIdExpr);
    }

    /**
     * Helper method that allows to get the list of attributeMappings from a given
     * featureTypeMapping node
     */
    static Node getChildByName(Node featureTypeMapping, String name) {
        NodeList childs = featureTypeMapping.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node child = childs.item(i);
            String nodeName = child.getNodeName();
            if (nodeName.equals(name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Helper method that allows to get the list of attributeMappings from a given
     * featureTypeMapping node
     */
    static Node getIdExpression(Node featureTypeMapping) {
        Node node = getChildByName(featureTypeMapping, "attributeMappings");
        NodeList nodeList = node.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node idExpr = getChildByName(nodeList.item(0), "idExpression");
            if (idExpr != null) return idExpr;
        }
        return null;
    }

    /**
     * Helper method that allows to get the unique DataStore id that should exists in the xml doc.
     * If it fails to get the id, then returns empty string.
     */
    static String getSourceDataStoreId(Document document) {
        // empty if there is no definition, first datastore id value (if existing)
        Node sourceDataStoresNode = getSourceDataStoresNode(document);
        if (sourceDataStoresNode != null) {
            NodeList dataStoresList = sourceDataStoresNode.getChildNodes();
            if (dataStoresList.getLength() > 0) {
                NodeList dataStoreChildNodes = dataStoresList.item(0).getChildNodes();
                for (int i = 0; i < dataStoreChildNodes.getLength(); i++) {
                    Node child = dataStoreChildNodes.item(i);
                    String nodeName = child.getNodeName();
                    if (nodeName.equals("id")) {
                        return child.getTextContent();
                    }
                }
            }
        }
        return "";
    }

    /**
     * Helper method that allows to get the a featureTypeMapping node based on the
     * targetElementValue argument.
     */
    static Node getFeatureTypeMapping(Document document, String targetElementValue) {
        NodeList ftmList = document.getElementsByTagName("FeatureTypeMapping");
        for (int i = 0; i < ftmList.getLength(); i++) {
            Node child = ftmList.item(i);
            NodeList ftmChildsList = child.getChildNodes();
            for (int j = 0; j < ftmChildsList.getLength(); j++) {
                Node ftmChild = ftmChildsList.item(j);
                String nodeName = ftmChild.getNodeName();
                if (nodeName.equals("targetElement")) {
                    if (ftmChild.getTextContent().equals(targetElementValue)) return child;
                }
            }
        }
        return null;
    }

    /**
     * Helper method that allows to get the an attributeMapping node for a given featureTypeMapping
     * and targetAttributeValue.
     */
    static Node getAttributeMapping(
            Document document, String targetElementValue, String targetAttributeValue) {
        Node featureTypeMappingNode = getFeatureTypeMapping(document, targetElementValue);
        if (featureTypeMappingNode != null) {
            NodeList ftmChilds = featureTypeMappingNode.getChildNodes();
            for (int i = 0; i < ftmChilds.getLength(); i++) {
                Node ftmChild = ftmChilds.item(i);
                String ftmChildNodeName = ftmChild.getNodeName();
                if (ftmChildNodeName.equals("attributeMappings")) {
                    NodeList attributeMappingList = ftmChild.getChildNodes();
                    for (int j = 0; j < attributeMappingList.getLength(); j++) {
                        Node amChild = attributeMappingList.item(j);
                        NodeList amChildChilds = amChild.getChildNodes();
                        for (int k = 0; k < amChildChilds.getLength(); k++) {
                            Node aChild = amChildChilds.item(k);
                            String nodeName = aChild.getNodeName();
                            if (nodeName.equals("targetAttribute")) {
                                if (aChild.getTextContent().equals(targetAttributeValue))
                                    return amChild;
                            }
                        }
                    }
                }
            }
        } else {
            throw new RuntimeException(
                    String.format("FeatureTypeMapping '%s' is unknown.", targetElementValue));
        }
        return null;
    }

    /**
     * Helper method that allows to get the number of linked attributesMappings occurencies in a
     * given featureTypeMapping. This helper allows to get the index number that will be used in
     * FEATURE_LINK[INDEX_NUMBER] references.
     */
    static int countLinkedAttributeMapping(Document document, String targetElementValue) {
        int ret = 1;
        Node featureTypeMappingNode = getFeatureTypeMapping(document, targetElementValue);
        if (featureTypeMappingNode != null) {
            NodeList ftmChilds = featureTypeMappingNode.getChildNodes();
            for (int i = 0; i < ftmChilds.getLength(); i++) {
                Node ftmChild = ftmChilds.item(i);
                String ftmChildNodeName = ftmChild.getNodeName();
                if (ftmChildNodeName.equals("attributeMappings")) {
                    NodeList attributeMappingList = ftmChild.getChildNodes();
                    for (int j = 0; j < attributeMappingList.getLength(); j++) {
                        Node amChild = attributeMappingList.item(j);
                        NodeList amChildChilds = amChild.getChildNodes();
                        for (int k = 0; k < amChildChilds.getLength(); k++) {
                            Node aChild = amChildChilds.item(k);
                            String nodeName = aChild.getNodeName();
                            if (nodeName.equals("targetAttribute")) {
                                if (aChild.getTextContent().contains("FEATURE_LINK")) ret++;
                            }
                        }
                    }
                }
            }
        } else {
            throw new RuntimeException(
                    String.format("FeatureTypeMapping '%s' is unknown.", targetElementValue));
        }
        return ret;
    }
}
