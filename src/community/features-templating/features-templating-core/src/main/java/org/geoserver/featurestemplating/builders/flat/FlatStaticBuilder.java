/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.flat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.geoserver.featurestemplating.builders.impl.StaticBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.featurestemplating.writers.GeoJSONWriter;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/** FlatStaticBuilder that concatenates its key to the parent key attribute */
public class FlatStaticBuilder extends StaticBuilder implements FlatBuilder {

    private AttributeNameHelper nameHelper;

    public FlatStaticBuilder(
            String key, JsonNode value, NamespaceSupport namespaces, String separator) {
        super(key, value, namespaces);
        nameHelper = new AttributeNameHelper(this.key, separator);
    }

    public FlatStaticBuilder(
            String key, String strValue, NamespaceSupport namespaces, String separator) {
        super(key, strValue, namespaces);
        nameHelper = new AttributeNameHelper(this.key, separator);
    }

    public FlatStaticBuilder(FlatStaticBuilder builder, boolean includeChildren) {
        super(builder, includeChildren);
        nameHelper = new AttributeNameHelper(this.key, builder.nameHelper.getSeparator());
    }

    @Override
    protected void evaluateInternal(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        GeoJSONWriter geoJsonWriter = (GeoJSONWriter) writer;
        if (strValue != null)
            geoJsonWriter.writeStaticContent(
                    nameHelper.getFinalAttributeName(context), strValue, nameHelper.getSeparator());
        else
            geoJsonWriter.writeStaticContent(
                    nameHelper.getFinalAttributeName(context),
                    staticValue,
                    nameHelper.getSeparator());
    }

    @Override
    public void setParentKey(String parentKey) {
        this.nameHelper.setParentKey(parentKey);
    }

    @Override
    public FlatStaticBuilder copy(boolean includeChildren) {
        return new FlatStaticBuilder(this, includeChildren);
    }
}
