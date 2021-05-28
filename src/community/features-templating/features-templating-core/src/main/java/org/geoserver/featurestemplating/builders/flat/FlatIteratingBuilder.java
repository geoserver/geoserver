/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.flat;

import java.io.IOException;
import java.util.List;
import org.geoserver.featurestemplating.builders.AbstractTemplateBuilder;
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
        nameHelper = new AttributeNameHelper(this.key, separator);
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (!rootCollection) {
            context = evaluateSource(context);
            if (context.getCurrentObj() != null) {
                if (context.getCurrentObj() instanceof List)
                    evaluateCollection(writer, context, false);
                else evaluateInternal(writer, context, 0, 1);
            }
        } else {
            if (evaluateFilter(context)) {
                for (TemplateBuilder child : children) {
                    AbstractTemplateBuilder abstractChild = (AbstractTemplateBuilder) child;
                    if (child instanceof FlatCompositeBuilder)
                        writer.startObject(abstractChild.getKey(), encodingHints);
                    child.evaluate(writer, context);
                    if (child instanceof FlatCompositeBuilder)
                        writer.endObject(abstractChild.getKey(), encodingHints);
                }
            }
        }
    }

    @Override
    public void evaluateCollection(
            TemplateOutputWriter writer, TemplateBuilderContext context, boolean iterateKey)
            throws IOException {

        List elements = (List) context.getCurrentObj();
        int elementsSize = elements.size();
        int actualIndex = 1;
        for (int i = 0; i < elementsSize; i++) {
            Object o = elements.get(i);
            TemplateBuilderContext childContext = new TemplateBuilderContext(o);
            childContext.setParent(context.getParent());
            if (evaluateFilter(childContext)) {
                evaluateInternal(writer, childContext, elementsSize, actualIndex);
                actualIndex++;
            }
        }
    }

    protected void evaluateInternal(
            TemplateOutputWriter writer,
            TemplateBuilderContext context,
            int elementsSize,
            int index)
            throws IOException {
        for (TemplateBuilder child : children) {
            ((FlatBuilder) child)
                    .setParentKey(
                            nameHelper.getCompleteIteratingAttributeName(elementsSize, index));
            child.evaluate(writer, context);
        }
    }

    @Override
    public void setParentKey(String parentKey) {
        this.nameHelper.setParentKey(parentKey);
    }
}
