package org.geoserver.featurestemplating.builders.jsonld;

import java.util.List;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.xml.sax.helpers.NamespaceSupport;

public class JsonLdIteratingBuilder extends IteratingBuilder {

    public JsonLdIteratingBuilder(String key, NamespaceSupport namespaces) {
        super(key, namespaces);
    }

    @Override
    protected boolean canWrite(TemplateBuilderContext context) {
        Object o = context.getCurrentObj();
        boolean result;
        if (o instanceof List) {
            result = canWriteList((List) o, context);
        } else {
            result = canWriteSingle(o, context);
        }
        return result;
    }

    private boolean canWriteList(List elements, TemplateBuilderContext context) {
        for (Object el : elements) {
            TemplateBuilderContext childContext = new TemplateBuilderContext(el);
            childContext.setParent(context.getParent());
            if (evaluateFilter(childContext)) return true;
        }
        return false;
    }

    private boolean canWriteSingle(Object element, TemplateBuilderContext context) {
        TemplateBuilderContext childContext = new TemplateBuilderContext(element);
        childContext.setParent(context.getParent());
        if (evaluateFilter(childContext)) return true;
        return false;
    }
}
