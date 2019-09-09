/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.data.service.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.metadata.data.dto.AttributeConfiguration;
import org.geoserver.metadata.data.dto.AttributeMappingConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeConfiguration;
import org.geoserver.metadata.data.dto.AttributeTypeMappingConfiguration;
import org.geoserver.metadata.data.dto.FieldTypeEnum;
import org.geoserver.metadata.data.dto.MappingTypeEnum;
import org.geoserver.metadata.data.dto.NamespaceConfiguration;
import org.geoserver.metadata.data.dto.OccurrenceEnum;
import org.geoserver.metadata.data.model.ComplexMetadataAttribute;
import org.geoserver.metadata.data.model.ComplexMetadataMap;
import org.geoserver.metadata.data.service.ConfigurationService;
import org.geoserver.metadata.data.service.GeonetworkXmlParser;
import org.geotools.util.Converters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Repository
public class GeonetworkXmlParserImpl implements GeonetworkXmlParser {

    private static final long serialVersionUID = -4931070325217885824L;

    @Autowired private ConfigurationService configService;

    private NamespaceContextImpl namespaceContext = new NamespaceContextImpl();;

    @PostConstruct
    public void setupNamespaces() {
        for (NamespaceConfiguration nsConfig :
                configService.getGeonetworkMappingConfiguration().getNamespaces()) {
            namespaceContext.register(nsConfig.getPrefix(), nsConfig.getURI());
        }
    }

    @Override
    public void parseMetadata(Document doc, ResourceInfo rInfo, ComplexMetadataMap metadataMap)
            throws IOException {
        for (AttributeMappingConfiguration attributeMapping :
                configService.getGeonetworkMappingConfiguration().getGeonetworkmapping()) {
            if (attributeMapping.getMappingType() != MappingTypeEnum.NATIVE) {
                clearAttribute(metadataMap, attributeMapping);
            }
        }
        for (AttributeMappingConfiguration attributeMapping :
                configService.getGeonetworkMappingConfiguration().getGeonetworkmapping()) {
            if (attributeMapping.getMappingType() == MappingTypeEnum.NATIVE) {
                addNativeAttribute(rInfo, attributeMapping, doc);
            } else {
                AttributeConfiguration att =
                        configService
                                .getMetadataConfiguration()
                                .findAttribute(attributeMapping.getGeoserver());
                if (att == null) {
                    throw new IOException(
                            "attribute "
                                    + attributeMapping.getGeoserver()
                                    + " not found in configuration");
                }
                addAttribute(metadataMap, attributeMapping, att, doc, null);
            }
        }
    }

    private void addNativeAttribute(
            ResourceInfo rInfo, AttributeMappingConfiguration attributeMapping, Document doc)
            throws IOException {
        List<String> values = findValues(doc, attributeMapping.getGeonetwork(), null);

        if (values.size() > 0) {
            try {
                Class<?> clazz =
                        PropertyUtils.getPropertyDescriptor(rInfo, attributeMapping.getGeoserver())
                                .getPropertyType();

                if (List.class.isAssignableFrom(clazz)) {
                    List<String> list = new ArrayList<String>();
                    for (String value : values) {
                        list.add(value);
                    }
                    @SuppressWarnings("unchecked")
                    List<Object> propList =
                            (List<Object>)
                                    PropertyUtils.getProperty(
                                            rInfo, attributeMapping.getGeoserver());
                    propList.clear();
                    propList.addAll(list);
                } else {
                    Object value =
                            clazz == null
                                    ? values.get(0)
                                    : Converters.convert(values.get(0), clazz);
                    BeanUtils.setProperty(rInfo, attributeMapping.getGeoserver(), value);
                }
            } catch (IllegalAccessException
                    | InvocationTargetException
                    | DOMException
                    | NoSuchMethodException e) {
                throw new IOException(e);
            }
        }
    }

    private void clearAttribute(
            ComplexMetadataMap metadataMap, AttributeMappingConfiguration attributeMapping) {
        metadataMap.delete(attributeMapping.getGeoserver());
    }

    private void addAttribute(
            ComplexMetadataMap metadataMap,
            AttributeMappingConfiguration attributeMapping,
            AttributeConfiguration attConfig,
            Document doc,
            Node node)
            throws IOException {

        if (FieldTypeEnum.COMPLEX.equals(attConfig.getFieldType())) {
            NodeList nodes = findNodes(attributeMapping, doc, node);
            switch (attConfig.getOccurrence()) {
                case SINGLE:
                    if (nodes != null && nodes.getLength() > 0) {
                        mapComplexNode(
                                metadataMap, attributeMapping, attConfig, doc, nodes.item(0));
                    }
                    break;
                case REPEAT:
                    if (nodes != null) {
                        for (int count = 0; count < nodes.getLength(); count++) {
                            mapComplexNode(
                                    metadataMap,
                                    attributeMapping,
                                    attConfig,
                                    doc,
                                    nodes.item(count));
                        }
                    }
                    break;
            }
        } else {
            List<String> values = findValues(doc, attributeMapping.getGeonetwork(), node);
            values.removeAll(Collections.singleton(null));
            switch (attConfig.getOccurrence()) {
                case SINGLE:
                    if (values != null && values.size() > 0) {
                        mapValue(metadataMap, attributeMapping, attConfig, doc, values.get(0));
                    }
                    break;
                case REPEAT:
                    if (values != null) {
                        for (String value : values) {
                            mapValue(metadataMap, attributeMapping, attConfig, doc, value);
                        }
                    }
                    break;
            }
        }
    }

    private void mapComplexNode(
            ComplexMetadataMap metadataMap,
            AttributeMappingConfiguration attributeMapping,
            AttributeConfiguration attConfig,
            Document doc,
            Node node)
            throws IOException {
        AttributeTypeMappingConfiguration typeMapping =
                configService.getGeonetworkMappingConfiguration().findType(attConfig.getTypename());
        if (typeMapping == null) {
            throw new IOException(
                    "type mapping " + attConfig.getTypename() + " not found in configuration");
        }
        AttributeTypeConfiguration type =
                configService.getMetadataConfiguration().findType(attConfig.getTypename());
        if (type == null) {
            throw new IOException(
                    "type " + attConfig.getTypename() + " not found in configuration");
        }
        ComplexMetadataMap submap;
        if (OccurrenceEnum.SINGLE.equals(attConfig.getOccurrence())) {
            submap = metadataMap.subMap(attributeMapping.getGeoserver());
        } else {
            int currentSize = metadataMap.size(attributeMapping.getGeoserver());
            submap = metadataMap.subMap(attributeMapping.getGeoserver(), currentSize);
        }
        for (AttributeMappingConfiguration aMapping : typeMapping.getMapping()) {
            clearAttribute(submap, aMapping);
        }
        for (AttributeMappingConfiguration aMapping : typeMapping.getMapping()) {
            AttributeConfiguration att = type.findAttribute(aMapping.getGeoserver());
            if (att == null) {
                throw new IOException(
                        "attribute "
                                + aMapping.getGeoserver()
                                + " not found in type "
                                + type.getTypename());
            }
            addAttribute(submap, aMapping, att, doc, node);
        }
    }

    private void mapValue(
            ComplexMetadataMap metadataMap,
            AttributeMappingConfiguration attributeMapping,
            AttributeConfiguration attConfig,
            Document doc,
            String value)
            throws IOException {
        ComplexMetadataAttribute<String> att;
        if (OccurrenceEnum.SINGLE.equals(attConfig.getOccurrence())) {
            att = metadataMap.get(String.class, attributeMapping.getGeoserver());
        } else {
            int currentSize = metadataMap.size(attributeMapping.getGeoserver());
            att = metadataMap.get(String.class, attributeMapping.getGeoserver(), currentSize);
        }
        att.setValue(value);
    }

    private Object find(Document doc, String geonetwork, Node node, QName type)
            throws XPathExpressionException {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(namespaceContext);
        XPathExpression expr = xpath.compile(geonetwork);
        Object result;
        if (node != null) {
            result = expr.evaluate(node, type);
        } else {
            result = expr.evaluate(doc, type);
        }
        return result;
    }

    private NodeList findNodes(
            AttributeMappingConfiguration attributeMapping, Document doc, Node node)
            throws IOException {
        try {
            return (NodeList)
                    find(doc, attributeMapping.getGeonetwork(), node, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        }
    }

    private List<String> findValues(Document doc, String geonetwork, Node node) throws IOException {
        List<String> result = new ArrayList<>();
        // try as NodeList
        try {
            NodeList nodeList = (NodeList) find(doc, geonetwork, node, XPathConstants.NODESET);
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    result.add(nodeList.item(i).getNodeValue());
                }
                return result;
            }
        } catch (XPathExpressionException e) {
            // do nothing
        }
        // try as String
        try {
            String value = (String) find(doc, geonetwork, node, XPathConstants.STRING);
            result.add(value);
            return result;
        } catch (XPathExpressionException e) {
            throw new IOException(e);
        }
    }
}
