/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfstemplating.builders.flat;

import java.io.IOException;
import org.geoserver.wfstemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.wfstemplating.writers.CommonJsonWriter;
import org.geoserver.wfstemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/** FlatDynamicBuilder that entirely delegates to the writer the key attribute handling */
public class FlatDynamicBuilder extends DynamicValueBuilder implements FlatBuilder {

    private AttributeNameHelper nameHelper;

    public FlatDynamicBuilder(String key, String expression, NamespaceSupport namespaces) {
        super(key, expression, namespaces);
        nameHelper = new AttributeNameHelper();
    }

    protected void writeValue(TemplateOutputWriter writer, Object value) throws IOException {

        ((CommonJsonWriter) writer)
                .writeElementNameAndValue(value, nameHelper.getFinalAttributeName());
    }

    @Override
    public void setParentKey(String parentKey) {
        this.nameHelper = new AttributeNameHelper(key, parentKey);
    }
}
