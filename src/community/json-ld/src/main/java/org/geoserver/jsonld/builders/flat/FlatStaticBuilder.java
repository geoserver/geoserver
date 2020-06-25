package org.geoserver.jsonld.builders.flat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.geoserver.jsonld.builders.impl.StaticBuilder;
import org.geoserver.jsonld.builders.impl.TemplateBuilderContext;
import org.geoserver.jsonld.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/** FlatStaticBuilder that concatenates its key to the parent key attribute */
public class FlatStaticBuilder extends StaticBuilder implements FlatBuilder {

    private AttributeNameHelper nameHelper;

    public FlatStaticBuilder(String key, JsonNode value, NamespaceSupport namespaces) {
        super(key, value, namespaces);
        nameHelper = new AttributeNameHelper();
    }

    public FlatStaticBuilder(String key, String strValue, NamespaceSupport namespaces) {
        super(key, strValue, namespaces);
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (evaluateFilter(context)) {
            if (strValue != null)
                writer.writeStaticContent(nameHelper.getFinalAttributeName(), strValue);
            else writer.writeStaticContent(nameHelper.getFinalAttributeName(), staticValue);
        }
    }

    public void setParentKey(String parentKey) {
        this.nameHelper = new AttributeNameHelper(key, parentKey);
    }
}
