/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.impl;

import java.io.IOException;
import java.util.List;
import org.geoserver.featurestemplating.builders.SourceBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This builder handle the writing of a Json array by invoking its children builders and setting the
 * context according to the $source specified in the template file.
 */
public class IteratingBuilder extends SourceBuilder {

    protected boolean isFeaturesField;

    public IteratingBuilder(String key, NamespaceSupport namespaces) {
        super(key, namespaces);
        this.isFeaturesField = key != null && key.equalsIgnoreCase("features");
    }

    @Override
    public void evaluate(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (!isFeaturesField) {
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
            writeKey(writer);
            writer.startArray();
            if (context.getCurrentObj() instanceof List) evaluateCollection(writer, context);
            else evaluateInternal(writer, context);
            writer.endArray();
        }
    }

    /**
     * Evaluate a context which is a List
     *
     * @param writer the template writer
     * @param context the context against which evaluate
     * @throws IOException
     */
    protected void evaluateCollection(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {

        List elements = (List) context.getCurrentObj();
        for (Object o : elements) {
            TemplateBuilderContext childContext = new TemplateBuilderContext(o);
            childContext.setParent(context.getParent());
            evaluateInternal(writer, childContext);
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
        return true;
    }
}
