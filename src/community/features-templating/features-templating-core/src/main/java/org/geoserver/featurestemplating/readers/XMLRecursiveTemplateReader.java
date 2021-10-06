/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.readers;

import static org.geoserver.featurestemplating.builders.EncodingHints.ENCODE_AS_ATTRIBUTE;
import static org.geoserver.featurestemplating.builders.EncodingHints.ITERATE_KEY;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
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
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.platform.resource.Resource;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class create TemplateBuilder out of a xml resource and is able to follow included templates
 * if they are found.
 */
public abstract class XMLRecursiveTemplateReader extends RecursiveTemplateResourceParser
        implements TemplateReader, AutoCloseable {

    protected TemplateBuilderMaker maker;
    protected Stack<StartElement> elementsStack;
    protected NamespaceSupport namespaceSupport;
    private InputStream inputSource;

    protected XMLEventReader reader;

    private static final String COLLECTION_ATTR = "gft:isCollection";

    private static final String FILTER_ATTR = "gft:filter";

    private static final String SOURCE_ATTR = "gft:source";

    private static final String TEMPLATE_ELEMENT = "gft:Template";

    protected static final String VENDOR_OPTIONS_EL = "gft:Options";

    private static final String INCLUDE_FLAT = "gft:includeFlat";

    private static final String INCLUDE = "$include";

    public XMLRecursiveTemplateReader(Resource resource, NamespaceSupport namespaceSupport)
            throws IOException {
        super(resource);
        this.resource = resource;
        this.elementsStack = new Stack<>();
        this.maker = new TemplateBuilderMaker();
        this.namespaceSupport = namespaceSupport;
        try {
            this.reader = getEventReader(resource);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    public XMLRecursiveTemplateReader(
            Resource resource, XMLRecursiveTemplateReader parent, NamespaceSupport namespaceSupport)
            throws IOException {
        super(resource, parent);
        this.elementsStack = new Stack<>();
        this.maker = new TemplateBuilderMaker();
        this.namespaceSupport = namespaceSupport;
        try {
            this.reader = getEventReader(resource);
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    @Override
    public RootBuilder getRootBuilder() {
        RootBuilder rootBuilder = new RootBuilder();
        try {
            iterateReader(rootBuilder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            close();
        }
        rootBuilder.setWatchers(getWatchers());
        return rootBuilder;
    }

    public void iterateReader(TemplateBuilder builder) throws IOException {
        try {
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) handleStartElement(event.asStartElement(), builder);
                else if (event.isCharacters()) {
                    Characters characters = event.asCharacters();
                    if (canParseTextContent(characters)) {
                        templateBuilderFromCharacterElement(event.asCharacters(), builder);
                    }
                } else if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();
                    boolean stopIteration = elementsStack.isEmpty();
                    // If the element stack is not empty means that we found
                    // a self closing element, that did not start a new iteration.
                    // Builders are then created here and this iteration will not stop
                    while (!elementsStack.isEmpty()) {
                        templateBuilderFromElement(elementsStack.pop(), builder, true);
                    }
                    if (!endElement.getName().toString().equals(INCLUDE_FLAT) && stopIteration)
                        break;
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
        } else if (data.startsWith(INCLUDE + "{") && data.endsWith("}")) {
            String path = data.substring(INCLUDE.length() + 1, data.length() - 1);
            TemplateBuilder parentForIncluded =
                    templateBuilderFromElement(element, currentParent, false);
            builderFromIncludedTemplate(resource, path, parentForIncluded);
        } else {
            TemplateBuilder leafBuilder;
            if (getAttributeValueIfPresent(element, COLLECTION_ATTR) != null)
                leafBuilder = createLeaf(data, null);
            else leafBuilder = createLeaf(data, element);
            currentParent.addChild(leafBuilder);
            addAttributeAsChildrenBuilder(element.getAttributes(), leafBuilder);
            iterateReader(currentParent);
        }
    }

    protected void handleStartElement(StartElement startElement, TemplateBuilder currentParent)
            throws IOException {
        if (startElement.getName().toString().equals(INCLUDE_FLAT)) {
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
            currentParent = templateBuilderFromElement(previous, currentParent, false);
        }
        return currentParent;
    }

    private TemplateBuilder templateBuilderFromElement(
            StartElement startElement, TemplateBuilder currentParent, boolean emptyNode) {
        if (startElement != null) {
            String isCollection = getAttributeValueIfPresent(startElement, COLLECTION_ATTR);
            String stringName = startElement.getName().toString();
            boolean collection =
                    isCollection != null && Boolean.valueOf(isCollection).booleanValue();
            boolean hasOwnOutput = hasOwnOutput(stringName);
            maker.collection(collection)
                    .name(stringName)
                    .namespaces(namespaceSupport)
                    .filter(getAttributeValueIfPresent(startElement, FILTER_ATTR))
                    .source(getAttributeValueIfPresent(startElement, SOURCE_ATTR))
                    .hasOwnOutput(hasOwnOutput)
                    .topLevelFeature(isRootOrManaged(currentParent));
            if (emptyNode) {
                maker.textContent("");
            }
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
        try (XMLRecursiveTemplateReader recursiveParser =
                getNewInstanceForRecursiveReading(included, this, namespaceSupport)) {
            recursiveParser.iterateReader(currentParent);
        }
    }

    protected abstract XMLRecursiveTemplateReader getNewInstanceForRecursiveReading(
            Resource included, XMLRecursiveTemplateReader parent, NamespaceSupport namespaceSupport)
            throws IOException;

    @Override
    public void close() {
        try {
            this.inputSource.close();
            this.reader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private XMLEventReader getEventReader(Resource resource) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
        this.inputSource = resource.in();
        return xmlInputFactory.createXMLEventReader(inputSource);
    }

    private void iterateVendorOptionsElement(TemplateBuilder builder) throws IOException {
        if (!(builder instanceof RootBuilder)) {
            throw new UnsupportedOperationException(
                    "Options can be defined only at the beginning of the template");
        }
        RootBuilder rootBuilder = (RootBuilder) builder;
        try {
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()) addVendorOption(event.asStartElement(), rootBuilder);
                else if (event.isCharacters()) {
                    Characters characters = event.asCharacters();
                    if (canParseTextContent(characters)) {
                        addVendorOption(characters, rootBuilder);
                    }
                } else if (event.isEndElement()) {
                    if (event.asEndElement().getName().toString().equals(VENDOR_OPTIONS_EL)) break;
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException(e);
        }
    }

    protected abstract void addVendorOption(StartElement element, RootBuilder builder);

    protected abstract void addVendorOption(Characters character, RootBuilder builder);

    protected abstract boolean hasOwnOutput(String elementName);

    private boolean isRootOrManaged(TemplateBuilder parent) {
        return parent instanceof RootBuilder || !((SourceBuilder) parent).hasOwnOutput();
    }

    private boolean canParseTextContent(Characters characters) {
        return !characters.isIgnorableWhiteSpace()
                && !characters.isWhiteSpace()
                && !characters.isEntityReference();
    }
}
