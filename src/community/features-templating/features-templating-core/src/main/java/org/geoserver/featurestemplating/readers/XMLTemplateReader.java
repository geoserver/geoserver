/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import static org.geoserver.featurestemplating.builders.EncodingHints.ENCODE_AS_ATTRIBUTE;
import static org.geoserver.featurestemplating.builders.EncodingHints.ITERATE_KEY;
import static org.geoserver.featurestemplating.builders.EncodingHints.NAMESPACES;
import static org.geoserver.featurestemplating.builders.EncodingHints.SCHEMA_LOCATION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class create a TemplateBuilder tree from an XML document. The XMLParser being used is not
 * namespaces aware.
 */
public class XMLTemplateReader implements TemplateReader {

    private RootBuilder rootBuilder;
    private TemplateBuilderMaker maker;
    private Stack<StartElement> elementsStack;
    private NamespaceSupport namespaceSupport;
    private List<StartElement> parsedElements;

    private static final String COLLECTION_ATTR = "gft:isCollection";

    private static final String FILTER_ATTR = "gft:filter";

    private static final String SOURCE_ATTR = "gft:source";

    private static final String FEATURE_COLL_ELEMENT = "wfs:FeatureCollection";

    private static final String NAMESPACE_PREFIX = "xmlns";

    private static final String SCHEMA_LOCATION_ATTR = "xsi:schemaLocation";

    public XMLTemplateReader(XMLEventReader reader, NamespaceSupport namespaceSupport)
            throws IOException {
        this.elementsStack = new Stack<>();
        this.maker = new TemplateBuilderMaker();
        this.namespaceSupport = namespaceSupport;
        this.parsedElements = new ArrayList<>();
        try {
            try {
                this.rootBuilder = new RootBuilder();
                iterateReader(reader, rootBuilder);
            } finally {
                reader.close();
            }
        } catch (XMLStreamException e) {
            throw new IOException("Failed to read XML template", e);
        }
    }

    @Override
    public RootBuilder getRootBuilder() {
        return rootBuilder;
    }

    private void iterateReader(XMLEventReader reader, TemplateBuilder builder)
            throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) handleStartElement(reader, event.asStartElement(), builder);
            else if (event.isCharacters()) {
                Characters characters = event.asCharacters();
                if (!characters.isIgnorableWhiteSpace()
                        && !characters.isWhiteSpace()
                        && !characters.isEntityReference())
                    templateBuilderFromCharacterElement(reader, event.asCharacters(), builder);
            } else if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                boolean alreadyParsed = alreadyParsed(endElement);
                if (!alreadyParsed && !elementsStack.isEmpty()) {
                    templateBuilderFromElement(elementsStack.pop(), builder);
                } else {
                    while (!elementsStack.isEmpty()) {
                        templateBuilderFromElement(elementsStack.pop(), builder);
                    }
                    break;
                }
            }
        }
    }

    private void templateBuilderFromCharacterElement(
            XMLEventReader reader, Characters characters, TemplateBuilder currentParent)
            throws XMLStreamException {
        String data = characters.getData();
        StartElement element = elementsStack.pop();
        TemplateBuilder leafBuilder;
        if (getAttributeValueIfPresent(element, COLLECTION_ATTR) != null)
            leafBuilder = createLeaf(data, null);
        else leafBuilder = createLeaf(data, element);
        parsedElements.add(element);
        currentParent.addChild(leafBuilder);
        addAttributeAsChildrenBuilder(element.getAttributes(), leafBuilder);
        iterateReader(reader, currentParent);
    }

    private void handleStartElement(
            XMLEventReader reader, StartElement startElement, TemplateBuilder currentParent)
            throws XMLStreamException {
        if (startElement.getName().toString().equals(FEATURE_COLL_ELEMENT)) {
            currentParent =
                    builderFromFeatureCollectionElement(startElement, (RootBuilder) currentParent);
            iterateReader(reader, currentParent);
        } else if (!elementsStack.isEmpty()) {
            StartElement previous = !elementsStack.isEmpty() ? elementsStack.pop() : null;
            currentParent = templateBuilderFromElement(previous, currentParent);
            parsedElements.add(previous);
            elementsStack.add(startElement);
            iterateReader(reader, currentParent);
        } else {
            elementsStack.add(startElement);
        }
    }

    private TemplateBuilder templateBuilderFromElement(
            StartElement startElement, TemplateBuilder currentParent) {
        if (startElement != null && !parsedElements.contains(startElement)) {
            String isCollection = getAttributeValueIfPresent(startElement, COLLECTION_ATTR);
            String qName = startElement.getName().toString();
            boolean collection =
                    isCollection != null && Boolean.valueOf(isCollection).booleanValue();
            maker.collection(collection)
                    .name(qName)
                    .namespaces(namespaceSupport)
                    .filter(getAttributeValueIfPresent(startElement, FILTER_ATTR))
                    .source(getAttributeValueIfPresent(startElement, SOURCE_ATTR));
            if (collection) {
                maker.encodingOption(ITERATE_KEY, "true");
            }
            TemplateBuilder parentBuilder = maker.build();
            Iterator<Attribute> attributeIterator = startElement.getAttributes();
            addAttributeAsChildrenBuilder(attributeIterator, parentBuilder);
            currentParent.addChild(parentBuilder);
            currentParent = parentBuilder;
        }
        return currentParent;
    }

    private TemplateBuilder createLeaf(String data, StartElement startElement) {
        maker.namespaces(namespaceSupport);
        if (startElement != null) {
            maker.name(strName(startElement.getName()));
            String filter = getAttributeValueIfPresent(startElement, FILTER_ATTR);
            if (filter == null) {
                maker.contentAndFilter(data);
            } else {
                maker.filter(filter).textContent(data);
            }
        }
        TemplateBuilder builder = maker.build();

        return builder;
    }

    private void addAttributeAsChildrenBuilder(
            Iterator<Attribute> attributes, TemplateBuilder parentBuilder) {
        while (attributes.hasNext()) {
            Attribute attribute = attributes.next();
            if (!attribute.isNamespace()) {
                if (canEncodeAttribute(attribute.getName())) {
                    maker.namespaces(namespaceSupport)
                            .name(strName(attribute.getName()))
                            .contentAndFilter(attribute.getValue())
                            .encodingOption(ENCODE_AS_ATTRIBUTE, true);
                    parentBuilder.addChild(maker.build());
                }
            }
        }
    }

    private String getAttributeValueIfPresent(StartElement startElement, String attributeName) {
        Attribute attribute = startElement.getAttributeByName(new QName(attributeName));
        if (attribute == null) {
            String[] attributeArr = attributeName.split(":");
            if (attributeArr.length > 1)
                attribute =
                        startElement.getAttributeByName(
                                new QName("", attributeArr[1], attributeArr[0]));
        }
        if (attribute == null) return null;
        return attribute.getValue();
    }

    private TemplateBuilder builderFromFeatureCollectionElement(
            StartElement startElementEvent, RootBuilder rootBuilder) {
        Iterator<Attribute> attributeIterator = startElementEvent.getAttributes();
        Map<String, String> namespaces = new HashMap<>();
        Map<String, String> schemaLocation = new HashMap<>();
        while (attributeIterator.hasNext()) {
            Attribute attribute = attributeIterator.next();
            QName name = attribute.getName();
            if (isNamespace(name)) {
                namespaces.put(localPart(name), attribute.getValue());
            } else if (isSchemaLocation(name)) {
                schemaLocation.put(localPart(attribute.getName()), attribute.getValue());
            }
        }
        rootBuilder.addEncodingHint(NAMESPACES, namespaces);
        rootBuilder.addEncodingHint(SCHEMA_LOCATION, schemaLocation);
        maker.collection(true)
                .name(startElementEvent.getName().toString())
                .namespaces(namespaceSupport)
                .rootCollection(true);
        TemplateBuilder builder = maker.build();
        rootBuilder.addChild(builder);
        return builder;
    }

    String localPart(QName name) {
        String[] nameAr = name.getLocalPart().split(":");
        String strName;
        if (nameAr.length > 1) {
            strName = nameAr[1];
        } else {
            strName = name.getLocalPart();
        }
        return strName;
    }

    String strName(QName name) {

        String[] nameAr = name.getLocalPart().split(":");
        String strName;
        if (nameAr.length > 1) {
            strName = nameAr[0] + ":" + nameAr[1];
        } else {
            strName = name.getLocalPart();
            String prefix = name.getPrefix();
            if (prefix != null && !prefix.trim().equals(""))
                strName = name.getPrefix() + ":" + strName;
        }
        return strName;
    }

    private boolean alreadyParsed(EndElement endElement) {
        long count =
                parsedElements
                        .stream()
                        .filter(se -> se.getName().equals(endElement.getName()))
                        .count();
        boolean alreadyParsed = count == 1;
        // additional check to avoid breaking an iteration in case of nodes with same name
        // where the nested one is a self closing or void node.
        if (count > 1) {
            alreadyParsed =
                    elementsStack.empty()
                            || !elementsStack.peek().getName().equals(endElement.getName());
        }
        return alreadyParsed;
    }

    private boolean isSchemaLocation(QName name) {
        return name.getLocalPart().equals(SCHEMA_LOCATION_ATTR)
                || name.getLocalPart().equals(SCHEMA_LOCATION_ATTR.split(":")[1]);
    }

    private boolean isNamespace(QName name) {
        return name.getLocalPart().startsWith(NAMESPACE_PREFIX)
                || name.getPrefix() != null && name.getPrefix().startsWith(NAMESPACE_PREFIX);
    }

    private boolean canEncodeAttribute(QName name) {
        boolean hasPrefix = name.getPrefix() != null && !name.getPrefix().equals("");
        String strName = name.getLocalPart();
        if (hasPrefix) strName = name.getPrefix() + ":" + strName;
        return !strName.equals(FILTER_ATTR)
                && !strName.equals(SOURCE_ATTR)
                && !strName.equals(COLLECTION_ATTR);
    }
}
