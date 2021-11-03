package org.geoserver.featurestemplating.readers;

import static org.geoserver.featurestemplating.builders.EncodingHints.NAMESPACES;
import static org.geoserver.featurestemplating.builders.EncodingHints.SCHEMA_LOCATION;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.VendorOptions;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.platform.resource.Resource;
import org.xml.sax.helpers.NamespaceSupport;

public class GMLTemplateReader extends XMLRecursiveTemplateReader {

    private static final String NAME_SPACES_EL = "gft:Namespaces";

    private static final String SCHEMA_LOCATION_EL = "gft:SchemaLocation";

    private static final String FEATURE_COLL_ELEMENT = "wfs:FeatureCollection";

    private static final String NAMESPACE_PREFIX = "xmlns";

    private static final String SCHEMA_LOCATION_ATTR = "xsi:schemaLocation";

    private static final String GML_MEMBER = "gml:featureMember";

    private static final String WFS_MEMBER = "wfs:member";

    public GMLTemplateReader(Resource resource, NamespaceSupport namespaceSupport)
            throws IOException {
        super(resource, namespaceSupport);
    }

    public GMLTemplateReader(
            Resource resource, XMLRecursiveTemplateReader parent, NamespaceSupport namespaceSupport)
            throws IOException {
        super(resource, parent, namespaceSupport);
    }

    @Override
    protected XMLRecursiveTemplateReader getNewInstanceForRecursiveReading(
            Resource included, XMLRecursiveTemplateReader parent, NamespaceSupport namespaceSupport)
            throws IOException {
        return new GMLTemplateReader(included, this, namespaceSupport);
    }

    @Override
    protected void addVendorOption(Characters character, RootBuilder builder) {
        // do nothing since all the GML options for the moment are passed as xml attributes
    }

    @Override
    protected void addVendorOption(StartElement element, RootBuilder builder) {
        String elementName = element.getName().toString();
        if (elementName.equals(NAME_SPACES_EL)) {
            Iterator<Attribute> attributeIterator = element.getAttributes();
            Map<String, String> namespaces = new HashMap<>();
            while (attributeIterator.hasNext()) {
                Attribute attr = attributeIterator.next();
                QName name = attr.getName();
                if (isNamespace(name)) namespaces.put(localPart(name), attr.getValue());
            }
            if (!namespaces.isEmpty())
                builder.addVendorOption(VendorOptions.NAMESPACES, namespaces);
        } else if (elementName.equals(SCHEMA_LOCATION_EL)) {
            Iterator<Attribute> attributeIterator = element.getAttributes();
            if (attributeIterator.hasNext()) {
                Attribute attribute = attributeIterator.next();
                QName name = attribute.getName();
                if (isSchemaLocation(name)) {
                    builder.addVendorOption(VendorOptions.SCHEMA_LOCATION, attribute.getValue());
                }
            }
        }
    }

    @Override
    protected void handleStartElement(StartElement startElement, TemplateBuilder currentParent)
            throws IOException {

        if (startElement.getName().toString().equals(FEATURE_COLL_ELEMENT)) {
            currentParent =
                    builderFromFeatureCollectionElement(startElement, (RootBuilder) currentParent);
            iterateReader(currentParent);
        } else {
            super.handleStartElement(startElement, currentParent);
        }
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
                .hasOwnOutput(false);
        TemplateBuilder builder = maker.build();
        rootBuilder.addChild(builder);
        return builder;
    }

    protected boolean isSchemaLocation(QName name) {
        return name.getLocalPart().equals(SCHEMA_LOCATION_ATTR)
                || name.getLocalPart().equals(SCHEMA_LOCATION_ATTR.split(":")[1]);
    }

    protected boolean isNamespace(QName name) {
        return name.getLocalPart().startsWith(NAMESPACE_PREFIX)
                || name.getPrefix() != null && name.getPrefix().startsWith(NAMESPACE_PREFIX);
    }

    @Override
    protected boolean hasOwnOutput(String elementName) {
        if (elementName == null) return true;
        return !(elementName.equals(GML_MEMBER) || elementName.equals(WFS_MEMBER));
    }
}
