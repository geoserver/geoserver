/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.flat;

import java.io.IOException;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/** FlatDynamicBuilder that entirely delegates to the writer the key attribute handling */
public class FlatDynamicBuilder extends DynamicValueBuilder implements FlatBuilder {

    private AttributeNameHelper nameHelper;

    public FlatDynamicBuilder(
            String key, String expression, NamespaceSupport namespaces, String separator) {
        super(key, expression, namespaces);
        nameHelper = new AttributeNameHelper(this.key, separator);
    }

    public FlatDynamicBuilder(FlatDynamicBuilder builder, boolean includeChildren) {
        super(builder, includeChildren);
        nameHelper = new AttributeNameHelper(this.key, builder.nameHelper.getSeparator());
    }

    @Override
    protected void writeValue(
            TemplateOutputWriter writer, Object value, TemplateBuilderContext context)
            throws IOException {
        writer.writeElementNameAndValue(
                nameHelper.getFinalAttributeName(context), value, getEncodingHints());
    }

    @Override
    public void setParentKey(String parentKey) {
        this.nameHelper.setParentKey(parentKey);
    }

    @Override
    public FlatDynamicBuilder copy(boolean includeChildren) {
        return new FlatDynamicBuilder(this, includeChildren);
    }
}
