/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.flat;

import java.io.IOException;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.opengis.feature.Feature;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * CompositeBuilder to generate a flat GeoJson output. It takes care of properly handle the
 * "properties" attribute name and to pass the key attribute if present to its children
 */
public class FlatCompositeBuilder extends CompositeBuilder implements FlatBuilder {

    private AttributeNameHelper attributeNameHelper;

    public FlatCompositeBuilder(String key, NamespaceSupport namespaces, String separator) {
        super(key, namespaces, false);
        attributeNameHelper = new AttributeNameHelper(this.key, separator);
    }

    public FlatCompositeBuilder(FlatCompositeBuilder original, boolean includeChildren) {
        super(original, includeChildren);
        attributeNameHelper =
                new AttributeNameHelper(this.key, original.attributeNameHelper.getSeparator());
    }

    public FlatCompositeBuilder(
            String key, NamespaceSupport namespaces, String separator, boolean topLevelComplex) {
        super(key, namespaces, topLevelComplex);
        attributeNameHelper = new AttributeNameHelper(this.key, separator);
    }

    @Override
    protected void evaluateChildren(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        Object o = context.getCurrentObj();
        addSkipObjectEncodingHint(context);
        String key = getKey(context);
        boolean isFeatureTypeBuilder = isFeatureTypeBuilder(o);
        if (isFeatureTypeBuilder
                || (key != null && key.equals(AttributeNameHelper.PROPERTIES_KEY))) {
            writer.startObject(key, encodingHints);
        }
        for (TemplateBuilder jb : children) {
            ((FlatBuilder) jb)
                    .setParentKey(attributeNameHelper.getCompleteCompositeAttributeName());
            jb.evaluate(writer, context);
        }
        if (isFeatureTypeBuilder(o)
                || (key != null && key.equals(AttributeNameHelper.PROPERTIES_KEY)))
            writer.endObject(key, encodingHints);
    }

    private boolean isFeatureTypeBuilder(Object o) {
        boolean result = false;
        if (o instanceof Feature) {
            Feature f = (Feature) o;
            result = getStrSource() != null && getSource().evaluate(f.getType()) == null;
        }
        return result;
    }

    @Override
    public void setParentKey(String parentKey) {
        this.attributeNameHelper.setParentKey(parentKey);
    }
}
