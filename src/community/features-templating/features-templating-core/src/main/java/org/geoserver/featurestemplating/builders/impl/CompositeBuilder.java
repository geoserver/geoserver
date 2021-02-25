/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Groups {@link StaticBuilder} and {@link DynamicValueBuilder}, invoke them and set, if found, the
 * context from them, according to $source value in template file.
 */
public class CompositeBuilder extends SourceBuilder {

    protected List<TemplateBuilder> children;

    public CompositeBuilder(String key, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.children = new LinkedList<>();
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
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
        writeKey(writer);
        writer.startObject();
        for (TemplateBuilder jb : children) {
            jb.evaluate(writer, context);
        }
        writer.endObject();
    }

    /**
     * Check if it is possible to write its content to the output
     *
     * @param context the current context
     * @return true if can write the output, else false
     */
    public boolean canWrite(TemplateBuilderContext context) {
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
    protected void writeKey(TemplateOutputWriter writer) throws IOException {
        if (key != null && !key.equals("")) writer.writeElementName(key);
    }
}
