/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.builders.flat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import org.geoserver.wfstemplating.builders.impl.StaticBuilder;
import org.geoserver.wfstemplating.builders.impl.TemplateBuilderContext;
import org.geoserver.wfstemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/** FlatStaticBuilder that concatenates its key to the parent key attribute */
public class FlatStaticBuilder extends StaticBuilder implements FlatBuilder {

    private AttributeNameHelper nameHelper;

    public FlatStaticBuilder(String key, JsonNode value, NamespaceSupport namespaces) {
        super(key, value, namespaces);
        nameHelper = new AttributeNameHelper();
    }

    public FlatStaticBuilder(String key, String strValue, NamespaceSupport namespaces) {
        super(key, strValue, namespaces);
    }

    @Override
    protected void evaluateInternal(TemplateOutputWriter writer, TemplateBuilderContext context)
            throws IOException {
        if (strValue != null)
            writer.writeStaticContent(nameHelper.getFinalAttributeName(), strValue);
        else writer.writeStaticContent(nameHelper.getFinalAttributeName(), staticValue);
    }

    public void setParentKey(String parentKey) {
        this.nameHelper = new AttributeNameHelper(getKey(), parentKey);
    }
}
