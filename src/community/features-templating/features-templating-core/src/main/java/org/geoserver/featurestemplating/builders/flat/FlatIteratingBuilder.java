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
import org.geotools.util.Converters;
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

    public FlatIteratingBuilder(
            String key, NamespaceSupport namespaces, String separator, boolean topLevelComplex) {
        super(key, namespaces, topLevelComplex);
        nameHelper = new AttributeNameHelper(this.key, separator);
    }

    public FlatIteratingBuilder(FlatIteratingBuilder builder, boolean includeChildren) {
        super(builder, includeChildren);
        nameHelper = new AttributeNameHelper(this.key, builder.nameHelper.getSeparator());
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (ownOutput) {
            context = evaluateSource(context);
            Object o = context.getCurrentObj();
            if (o != null) {
                if (o instanceof List) {
                    evaluateCollection(writer, (List) o, context.getParent(), false);
                } else if (o.getClass().isArray()) {
                    List list = Converters.convert(o, List.class);
                    evaluateCollection(writer, list, context.getParent(), false);
                } else {
                    evaluateInternal(writer, context, 0, 1);
                }
            }
        } else {
            if (evaluateFilter(context)) {
                addSkipObjectEncodingHint(context);
                for (TemplateBuilder child : children) {
                    AbstractTemplateBuilder abstractChild = (AbstractTemplateBuilder) child;
                    if (child instanceof FlatCompositeBuilder)
                        writer.startObject(abstractChild.getKey(context), encodingHints);
                    child.evaluate(writer, context);
                    if (child instanceof FlatCompositeBuilder)
                        writer.endObject(abstractChild.getKey(context), encodingHints);
                }
            }
        }
    }

    @Override
    public void evaluateCollection(
            TemplateOutputWriter writer,
            List elements,
            TemplateBuilderContext parent,
            boolean iterateKey)
            throws IOException {

        int elementsSize = elements.size();
        int actualIndex = 1;
        for (int i = 0; i < elementsSize; i++) {
            Object o = elements.get(i);
            TemplateBuilderContext childContext = new TemplateBuilderContext(o);
            childContext.setParent(parent);
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

    @Override
    public FlatIteratingBuilder copy(boolean includeChildren) {
        return new FlatIteratingBuilder(this, includeChildren);
    }
}
