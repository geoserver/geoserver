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
import org.xml.sax.helpers.NamespaceSupport;

/**
 * CompositeBuilder to generate a flat GeoJson output. It takes care of properly handle the
 * "properties" attribute name and to pass the key attribute if present to its children
 */
public class FlatCompositeBuilder extends CompositeBuilder implements FlatBuilder {

    private AttributeNameHelper attributeNameHelper;

    public FlatCompositeBuilder(String key, NamespaceSupport namespaces, String separator) {
        super(key, namespaces);
        attributeNameHelper = new AttributeNameHelper(getKey(), separator);
    }

    @Override
    protected void evaluateChildren(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (getKey() != null && getKey().equals(AttributeNameHelper.PROPERTIES_KEY)) {
            writeKey(writer);
            writer.startObject();
        }
        for (TemplateBuilder jb : children) {
            ((FlatBuilder) jb)
                    .setParentKey(attributeNameHelper.getCompleteCompositeAttributeName());
            jb.evaluate(writer, context);
        }
        if (getKey() != null && getKey().equals(AttributeNameHelper.PROPERTIES_KEY))
            writer.endObject();
    }

    public void setParentKey(String parentKey) {
        this.attributeNameHelper.setParentKey(parentKey);
    }
}
