/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.flat;

import java.io.IOException;
import java.util.List;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.IteratingBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * IteratingBuilder to produce a flat GeoJSON output. Takes care of passing its attribute key to its
 * children, adding an index to the attribute name if needed.
 */
public class FlatIteratingBuilder extends IteratingBuilder implements FlatBuilder {

    private AttributeNameHelper nameHelper;

    public FlatIteratingBuilder(String key, NamespaceSupport namespaces, String separator) {
        super(key, namespaces);
        nameHelper = new AttributeNameHelper(getKey(), separator);
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (!isFeaturesField) {
            context = evaluateSource(context);
            if (context.getCurrentObj() != null) {
                if (context.getCurrentObj() instanceof List) evaluateCollection(writer, context);
                else evaluateInternal(writer, context, 0, 0);
            }
        } else {
            if (evaluateFilter(context)) {
                for (TemplateBuilder child : children) {
                    if (child instanceof FlatCompositeBuilder) writer.startObject();
                    child.evaluate(writer, context);
                    if (child instanceof FlatCompositeBuilder) writer.endObject();
                }
            }
        }
    }

    @Override
    public void evaluateCollection(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {

        List elements = (List) context.getCurrentObj();
        int elementsSize = elements.size();
        for (int i = 0; i < elementsSize; i++) {
            Object o = elements.get(i);
            TemplateBuilderContext childContext = new TemplateBuilderContext(o);
            childContext.setParent(context.getParent());
            evaluateInternal(writer, childContext, elementsSize, i);
        }
    }

    protected void evaluateInternal(
            TemplateOutputWriter writer,
            TemplateBuilderContext context,
            int elementsSize,
            int index)
            throws IOException {
        if (evaluateFilter(context)) {
            for (TemplateBuilder child : children) {
                ((FlatBuilder) child)
                        .setParentKey(
                                nameHelper.getCompleteIteratingAttributeName(elementsSize, index));
                child.evaluate(writer, context);
            }
        }
    }

    @Override
    public void setParentKey(String parentKey) {
        this.nameHelper.setParentKey(parentKey);
    }
}
