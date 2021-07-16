/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.visitors.TemplateVisitor;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Groups {@link StaticBuilder} and {@link DynamicValueBuilder}, invoke them and set, if found, the
 * context from them, according to $source value in template file.
 */
public class CompositeBuilder extends SourceBuilder {

    protected List<TemplateBuilder> children;

    public CompositeBuilder(String key, NamespaceSupport namespaces, boolean topLevelComplex) {
        super(key, namespaces, topLevelComplex);
        this.children = new LinkedList<>();
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        addSkipObjectEncodingHint(context);
        context = evaluateSource(context);
        Object o = context.getCurrentObj();
        if (o != null && evaluateFilter(context) && canWrite(context)) {
            evaluateChildren(writer, context);
        }
    }

    /**
     * Start the evaluation of the builder children
     *
     * @param writer the template output writer
     * @param context the context to be passed to the children
     * @throws IOException
     */
    protected void evaluateChildren(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (ownOutput) writer.startObject(getKey(context), encodingHints);
        for (TemplateBuilder jb : children) {
            jb.evaluate(writer, context);
        }
        if (ownOutput) writer.endObject(getKey(context), encodingHints);
    }

    /**
     * Check if it is possible to write its content to the output. By default, returns true if at
     * least one of the child builders has a non null value
     *
     * @param context the current context
     * @return true if can write the output, else false
     */
    public boolean canWrite(TemplateBuilderContext context) {
        List<TemplateBuilder> filtered =
                children.stream()
                        .filter(b -> b instanceof DynamicValueBuilder || b instanceof SourceBuilder)
                        .collect(Collectors.toList());
        if (filtered.size() == children.size()) {
            int falseCounter = 0;
            for (TemplateBuilder b : filtered) {
                if (b instanceof CompositeBuilder) {
                    if (!((CompositeBuilder) b).canWrite(context)) falseCounter++;
                } else if (b instanceof IteratingBuilder) {
                    if (!((IteratingBuilder) b).canWrite(context)) falseCounter++;
                } else {
                    if (!((DynamicValueBuilder) b).checkNotNullValue(context)) falseCounter++;
                }
            }
            if (falseCounter == filtered.size()) return false;
        }
        return true;
    }

    @Override
    public void addChild(TemplateBuilder children) {
        this.children.add(children);
    }

    @Override
    public List<TemplateBuilder> getChildren() {
        return children;
    }

    @Override
    public Object accept(TemplateVisitor visitor, Object value) {
        return visitor.visit(this, value);
    }
}
