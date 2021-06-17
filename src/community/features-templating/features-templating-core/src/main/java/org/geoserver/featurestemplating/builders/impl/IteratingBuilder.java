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

    protected boolean rootCollection;

    public IteratingBuilder(String key, NamespaceSupport namespaces) {
        super(key, namespaces);
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (!rootCollection) {
            context = evaluateSource(context);
            if (context.getCurrentObj() != null) {
                evaluateNonFeaturesField(writer, context);
            }
        } else {
            evaluateInternal(writer, context);
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
            String key = getKey();
            Object o = context.getCurrentObj();
            boolean isList = o instanceof List;
            boolean isArray = o != null && o.getClass().isArray();
            // if this is not a context as list or we don't have
            // iterate key hint we start the array and encode the key once here
            boolean wrapInArray = !iterateKey || !isList || !isArray;
            if (wrapInArray) writer.startArray(key, encodingHints);

            if (isList) {
                evaluateCollection(
                        writer, (List) context.getCurrentObj(), context.getParent(), iterateKey);
            } else if (isArray) {
                List list = Converters.convert(o, List.class);
                evaluateCollection(writer, list, context.getParent(), iterateKey);
            } else {
                evaluateInternal(writer, context);
            }

            if (wrapInArray) writer.endArray(key, encodingHints);
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
                String key = getKey();
                // repeat the key attribute according to the hint
                if (iterateKey && !rootCollection) writer.startArray(key, encodingHints);
                for (TemplateBuilder child : children) {
                    child.evaluate(writer, childContext);
                }
                if (iterateKey && !rootCollection) writer.endArray(key, encodingHints);
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
    protected void evaluateInternal(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (evaluateFilter(context)) {
            for (TemplateBuilder child : children) {
                child.evaluate(writer, context);
            }
        }
    }

    protected boolean canWrite(TemplateBuilderContext context) {
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

    public boolean isRootCollection() {
        return rootCollection;
    }

    public void setRootCollection(boolean rootCollection) {
        this.rootCollection = rootCollection;
    }

    private boolean isIterateKey() {
        Object iterateKey = getEncodingHints().get(ITERATE_KEY);
        return iterateKey != null && Boolean.valueOf(iterateKey.toString()).booleanValue();
    }

    @Override
    public Object accept(TemplateVisitor visitor, Object value) {
        return visitor.visit(this, value);
    }
}
