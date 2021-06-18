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
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderMaker;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.platform.resource.Resource;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class create TemplateBuilder out of a xml resource and is able to follow included templates
 * if they are found.
 */
public class XMLRecursiveReader extends RecursiveTemplateResourceParser implements AutoCloseable {

    private TemplateBuilderMaker maker;
    private Stack<StartElement> elementsStack;
    private NamespaceSupport namespaceSupport;
    private List<StartElement> parsedElements;

    private XMLEventReader reader;

    private static final String COLLECTION_ATTR = "gft:isCollection";

    private static final String FILTER_ATTR = "gft:filter";

    private static final String SOURCE_ATTR = "gft:source";

    private static final String FEATURE_COLL_ELEMENT = "wfs:FeatureCollection";

    private static final String TEMPLATE_ELEMENT = "gft:Template";

    private static final String VENDOR_OPTIONS_EL = "gft:VendorOptions";

    private static final String NAME_SPACES_EL = "gft:Namespaces";

    private static final String SCHEMA_LOCATION_EL = "gft:SchemaLocation";

    private static final String NAMESPACE_PREFIX = "xmlns";

    private static final String SCHEMA_LOCATION_ATTR = "xsi:schemaLocation";

    private static final String INCLUDE_FLAT = "gft:includeFlat";

    private static final String INCLUDE = "$include";

    private static final String GML_MEMBER = "gml:featureMember";

    private static final String WFS_MEMBER = "wfs:member";

    public XMLRecursiveReader(Resource resource, NamespaceSupport namespaceSupport)
            throws IOException {
        super(resource);
        this.resource = resource;
        this.elementsStack = new Stack<>();
        this.maker = new TemplateBuilderMaker();
        this.namespaceSupport = namespaceSupport;
        this.parsedElements = new ArrayList<>();
        try {
            this.reader = getEventReader(resource);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    public XMLRecursiveReader(
            Resource resource, XMLRecursiveReader parent, NamespaceSupport namespaceSupport)
            throws IOException {
        super(resource, parent);
        this.elementsStack = new Stack<>();
        this.maker = new TemplateBuilderMaker();
        this.namespaceSupport = namespaceSupport;
        this.parsedElements = new ArrayList<>();
        try {
            this.reader = getEventReader(resource);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    public void iterateReader(TemplateBuilder builder) throws IOException {
        try {
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) handleStartElement(event.asStartElement(), builder);
                else if (event.isCharacters()) {
                    Characters characters = event.asCharacters();
                    if (!characters.isIgnorableWhiteSpace()
                            && !characters.isWhiteSpace()
                            && !characters.isEntityReference())
                        templateBuilderFromCharacterElement(event.asCharacters(), builder);
                } else if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();
                    boolean alreadyParsed = alreadyParsed(endElement);
                    if (!alreadyParsed && !elementsStack.isEmpty()) {
                        templateBuilderFromElement(elementsStack.pop(), builder);
                    } else {
                        while (!elementsStack.isEmpty()) {
                            templateBuilderFromElement(elementsStack.pop(), builder);
                        }
                        if (!endElement.getName().toString().equals(INCLUDE_FLAT)) break;
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private void templateBuilderFromCharacterElement(
            Characters characters, TemplateBuilder currentParent) throws IOException {
        String data = characters.getData();
        StartElement element = elementsStack.pop();
        if (element.getName().toString().equals(INCLUDE_FLAT)) {
            builderFromIncludedTemplate(resource, data, currentParent);
            parsedElements.add(element);
        } else if (data.startsWith(INCLUDE + "{") && data.endsWith("}")) {
            String path = data.substring(INCLUDE.length() + 1, data.length() - 1);
            TemplateBuilder parentForIncluded = templateBuilderFromElement(element, currentParent);
            builderFromIncludedTemplate(resource, path, parentForIncluded);
        } else {
            TemplateBuilder leafBuilder;
            if (getAttributeValueIfPresent(element, COLLECTION_ATTR) != null)
                leafBuilder = createLeaf(data, null);
            else leafBuilder = createLeaf(data, element);
            parsedElements.add(element);
            currentParent.addChild(leafBuilder);
            addAttributeAsChildrenBuilder(element.getAttributes(), leafBuilder);
            iterateReader(currentParent);
        }
    }

    private void handleStartElement(StartElement startElement, TemplateBuilder currentParent)
            throws IOException {
        if (startElement.getName().toString().equals(FEATURE_COLL_ELEMENT)) {
            currentParent =
                    builderFromFeatureCollectionElement(startElement, (RootBuilder) currentParent);
            iterateReader(currentParent);
        } else if (startElement.getName().toString().equals(INCLUDE_FLAT)) {
            currentParent = getBuilderFromLastElInStack(currentParent);
            elementsStack.add(startElement);
            String data = getAttributeValueIfPresent(startElement, SOURCE_ATTR);
            if (data != null) builderFromIncludedTemplate(resource, data, currentParent);
            else iterateReader(currentParent);
        } else if (!elementsStack.isEmpty()) {
            currentParent = getBuilderFromLastElInStack(currentParent);
            elementsStack.add(startElement);
            iterateReader(currentParent);
        } else if (startElement.getName().toString().equals(VENDOR_OPTIONS_EL)) {
            iterateVendorOptionsElement(currentParent);
        } else if (!startElement.getName().toString().equals(TEMPLATE_ELEMENT)) {
            elementsStack.add(startElement);
        }
    }

    private TemplateBuilder getBuilderFromLastElInStack(TemplateBuilder currentParent) {
        StartElement previous = !elementsStack.isEmpty() ? elementsStack.pop() : null;
        if (previous != null) {
            currentParent = templateBuilderFromElement(previous, currentParent);
            parsedElements.add(previous);
        }
        return currentParent;
    }

    private TemplateBuilder templateBuilderFromElement(
            StartElement startElement, TemplateBuilder currentParent) {
        if (startElement != null && !parsedElements.contains(startElement)) {
            String isCollection = getAttributeValueIfPresent(startElement, COLLECTION_ATTR);
            String qName = startElement.getName().toString();
            boolean collection =
                    isCollection != null && Boolean.valueOf(isCollection).booleanValue();
            boolean managed = qName.equals(GML_MEMBER) || qName.equals(WFS_MEMBER);
            maker.collection(collection)
                    .name(qName)
                    .namespaces(namespaceSupport)
                    .filter(getAttributeValueIfPresent(startElement, FILTER_ATTR))
                    .source(getAttributeValueIfPresent(startElement, SOURCE_ATTR))
                    .managedBuilder(managed)
                    .topLevelFeature(isRootOrManaged(currentParent));
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
            if (canEncodeAttribute(attribute.getName())) {
                maker.namespaces(namespaceSupport)
                        .name(strName(attribute.getName()))
                        .contentAndFilter(attribute.getValue())
                        .encodingOption(ENCODE_AS_ATTRIBUTE, true);
                parentBuilder.addChild(maker.build());
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
        String schemaLocation = null;
        while (attributeIterator.hasNext()) {
            Attribute attribute = attributeIterator.next();
            QName name = attribute.getName();
            if (isNamespace(name)) {
                namespaces.put(localPart(name), attribute.getValue());
            } else if (isSchemaLocation(name)) {
                schemaLocation = attribute.getValue();
            }
        }
        rootBuilder.addEncodingHint(NAMESPACES, namespaces);
        if (schemaLocation != null) rootBuilder.addEncodingHint(SCHEMA_LOCATION, schemaLocation);
        maker.collection(true)
                .name(startElementEvent.getName().toString())
                .namespaces(namespaceSupport)
                .managedBuilder(true);
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

    private void builderFromIncludedTemplate(
            Resource resource, String data, TemplateBuilder currentParent) throws IOException {
        Resource included = getResource(resource.parent(), data);
        try (XMLRecursiveReader recursiveParser =
                new XMLRecursiveReader(included, this, namespaceSupport)) {
            recursiveParser.iterateReader(currentParent);
        }
    }

    @Override
    public void close() {
        try {
            this.reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private XMLEventReader getEventReader(Resource resource) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        return xmlInputFactory.createXMLEventReader(resource.in());
    }

    private TemplateBuilder retrieveLastAddedTemplateBuilder(TemplateBuilder currentBuilder) {
        while (!currentBuilder.getChildren().isEmpty()) {
            int size = currentBuilder.getChildren().size();
            currentBuilder = currentBuilder.getChildren().get(size - 1);
        }
        return currentBuilder;
    }

    private void iterateVendorOptionsElement(TemplateBuilder builder) throws IOException {
        try {
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) addVendorOption(event.asStartElement(), builder);
                else if (event.isEndElement()) {
                    if (event.asEndElement().getName().toString().equals(VENDOR_OPTIONS_EL)) break;
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    private void addVendorOption(StartElement element, TemplateBuilder builder) {
        if (element.getName().toString().equals(NAME_SPACES_EL)) {
            Iterator<Attribute> attributeIterator = element.getAttributes();
            Map<String, String> namespaces = new HashMap<>();
            while (attributeIterator.hasNext()) {
                Attribute attr = attributeIterator.next();
                QName name = attr.getName();
                if (isNamespace(name)) namespaces.put(localPart(name), attr.getValue());
            }
            if (!namespaces.isEmpty())
                ((RootBuilder) builder).addVendorOption(VendorOptions.NAMESPACES, namespaces);
        } else if (element.getName().toString().equals(SCHEMA_LOCATION_EL)) {
            Iterator<Attribute> attributeIterator = element.getAttributes();
            if (attributeIterator.hasNext()) {
                Attribute attribute = attributeIterator.next();
                QName name = attribute.getName();
                if (isSchemaLocation(name)) {
                    ((RootBuilder) builder)
                            .addVendorOption(VendorOptions.SCHEMA_LOCATION, attribute.getValue());
                }
            }
        }
    }

    private boolean isRootOrManaged(TemplateBuilder parent) {
        return parent instanceof RootBuilder || ((SourceBuilder) parent).isManaged();
    }
}
