package org.geoserver.featurestemplating.readers;

import static org.geoserver.featurestemplating.builders.VendorOptions.LINK;
import static org.geoserver.featurestemplating.builders.VendorOptions.SCRIPT;
import static org.geoserver.featurestemplating.builders.VendorOptions.STYLE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.platform.resource.Resource;
import org.xml.sax.helpers.NamespaceSupport;

public class XHTMLTemplateReader extends XMLRecursiveTemplateReader {

    protected Stack<String> optionsNamesStack;

    public XHTMLTemplateReader(Resource resource, NamespaceSupport namespaceSupport)
            throws IOException {
        super(resource, namespaceSupport);
        optionsNamesStack = new Stack<>();
    }

    public XHTMLTemplateReader(
            Resource resource, XMLRecursiveTemplateReader parent, NamespaceSupport namespaceSupport)
            throws IOException {
        super(resource, parent, namespaceSupport);
        optionsNamesStack = new Stack<>();
    }

    @Override
    protected XMLRecursiveTemplateReader getNewInstanceForRecursiveReading(
            Resource included, XMLRecursiveTemplateReader parent, NamespaceSupport namespaceSupport)
            throws IOException {
        return new XHTMLTemplateReader(included, this, namespaceSupport);
    }

    @Override
    protected void addVendorOption(StartElement element, RootBuilder builder) {
        String elementName = element.getName().toString();
        if (elementName.equalsIgnoreCase(SCRIPT) || elementName.equalsIgnoreCase(STYLE)) {
            optionsNamesStack.add(elementName);
        } else if (elementName.equalsIgnoreCase(LINK)) {
            Iterator<Attribute> attributeIterator = element.getAttributes();
            Map<QName, String> attrs = new HashMap<>();
            List<Attribute> attributes = new ArrayList<>();
            while (attributeIterator.hasNext()) {
                Attribute attr = attributeIterator.next();
                attributes.add(attr);
            }
            if (!attributes.isEmpty()) {
                builder.addVendorOption(LINK + element.hashCode(), attributes);
            }
        }
    }

    @Override
    protected void addVendorOption(Characters character, RootBuilder builder) {
        if (!optionsNamesStack.isEmpty()) {
            String name = optionsNamesStack.pop();
            String optionName = null;
            if (name.equalsIgnoreCase(STYLE)) optionName = STYLE;
            else if (name.equalsIgnoreCase(SCRIPT)) optionName = SCRIPT;
            if (optionName != null) {
                String content = character.getData();
                builder.addVendorOption(optionName, content);
            }
        }
    }

    @Override
    protected boolean hasOwnOutput(String elementName) {
        return true;
    }
}
