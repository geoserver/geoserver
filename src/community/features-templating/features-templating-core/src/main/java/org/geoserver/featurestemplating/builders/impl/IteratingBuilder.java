/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import static org.geoserver.featurestemplating.builders.EncodingHints.ITERATE_KEY;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.List;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.visitors.TemplateVisitor;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.geotools.util.Converters;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This builder handle the writing of a Json array by invoking its children builders and setting the
 * context according to the $source specified in the template file.
 */
public class IteratingBuilder extends SourceBuilder {

    public IteratingBuilder(String key, NamespaceSupport namespaces) {
        super(key, namespaces, false);
    }

    public IteratingBuilder(String key, NamespaceSupport namespaces, boolean topLevelComplex) {
        super(key, namespaces, topLevelComplex);
    }

    public IteratingBuilder(IteratingBuilder iteratingBuilder, boolean includeChildren) {
        super(iteratingBuilder, includeChildren);
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (ownOutput) {
            context = evaluateSource(context);
            if (context.getCurrentObj() != null) {
                evaluateNonFeaturesField(writer, context);
            }
        } else {
            evaluateInternal(writer, context, false);
        }
    }

    /**
     * Method used to evaluate if this IteratingBuilder is not the root one
     *
     * @param writer the template writer
     * @param context the current context
     * @throws IOException
     */
    protected void evaluateNonFeaturesField(
            TemplateOutputWriter writer, TemplateBuilderContext context) throws IOException {
        if (canWrite(context)) {
            boolean iterateKey = isIterateKey();
            String key = getKey(context);
            Object o = context.getCurrentObj();
            boolean isList = o instanceof List;
            boolean isArray = o != null && o.getClass().isArray();
            // if this is not a context as list and we don't have
            // iterate key hint we start the array and encode the key once here
            if (!iterateKey && hasOwnOutput()) writer.startArray(key, this.encodingHints);

            if (isList) {
                evaluateCollection(
                        writer, (List) context.getCurrentObj(), context.getParent(), iterateKey);
            } else if (isArray) {
                List list = Converters.convert(o, List.class);
                evaluateCollection(writer, list, context.getParent(), iterateKey);
            } else {
                evaluateInternal(writer, context, iterateKey);
            }

            if (!iterateKey && hasOwnOutput()) writer.endArray(key, this.encodingHints);
        }
    }

    /**
     * Evaluate a context which is a List
     *
     * @param writer the template writer
     * @param elements
     * @param parent
     * @throws IOException
     */
    protected void evaluateCollection(
            TemplateOutputWriter writer,
            List elements,
            TemplateBuilderContext parent,
            boolean iterateKey)
            throws IOException {

        for (Object o : elements) {
            TemplateBuilderContext childContext = new TemplateBuilderContext(o);
            childContext.setParent(parent);
            if (evaluateFilter(childContext)) {
                String key = getKey(parent);
                // repeat the key attribute according to the hint
                if (iterateKey && hasOwnOutput()) writer.startArray(key, encodingHints);
                for (TemplateBuilder child : children) {
                    child.evaluate(writer, childContext);
                }
                if (iterateKey && hasOwnOutput()) writer.endArray(key, encodingHints);
            }
        }
    }

    /**
     * Triggers the children evaluation
     *
     * @param writer the template writer
     * @param context the current context
     * @throws IOException
     */
    protected void evaluateInternal(
            TemplateOutputWriter writer, TemplateBuilderContext context, boolean iterateKey)
            throws IOException {
        if (evaluateFilter(context)) {
            String key = getKey(context);
            if (iterateKey && hasOwnOutput()) writer.startArray(key, encodingHints);
            for (TemplateBuilder child : children) {
                child.evaluate(writer, context);
            }
            if (iterateKey && hasOwnOutput()) writer.endArray(key, encodingHints);
        }
    }

    public boolean canWrite(TemplateBuilderContext context) {
        Object o = context.getCurrentObj();
        boolean result;
        if (o instanceof List) {
            result = canWriteList((List) o, context);
        } else if (o != null && o.getClass().isArray()) {
            result = canWriteArray(o, context);
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

    private boolean canWriteArray(Object array, TemplateBuilderContext context) {
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            Object item = Array.get(array, i);
            TemplateBuilderContext childContext = new TemplateBuilderContext(item);
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

    private boolean isIterateKey() {
        Object iterateKey = getEncodingHints().get(ITERATE_KEY);
        return iterateKey != null && Boolean.valueOf(iterateKey.toString()).booleanValue();
    }

    @Override
    public Object accept(TemplateVisitor visitor, Object value) {
        return visitor.visit(this, value);
    }

    @Override
    public IteratingBuilder copy(boolean includeChildren) {
        return new IteratingBuilder(this, includeChildren);
    }
}
