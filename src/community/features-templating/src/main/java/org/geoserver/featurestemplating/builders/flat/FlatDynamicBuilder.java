/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.flat;

import java.io.IOException;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.writers.CommonJsonWriter;
import org.geoserver.featurestemplating.writers.TemplateOutputWriter;
import org.xml.sax.helpers.NamespaceSupport;

/** FlatDynamicBuilder that entirely delegates to the writer the key attribute handling */
public class FlatDynamicBuilder extends DynamicValueBuilder implements FlatBuilder {

    private AttributeNameHelper nameHelper;

    public FlatDynamicBuilder(
            String key, String expression, NamespaceSupport namespaces, String separator) {
        super(key, expression, namespaces);
        nameHelper = new AttributeNameHelper(getKey(), separator);
    }

    protected void writeValue(TemplateOutputWriter writer, Object value) throws IOException {

        ((CommonJsonWriter) writer)
                .writeElementNameAndValue(value, nameHelper.getFinalAttributeName());
    }

    @Override
    public void setParentKey(String parentKey) {
        this.nameHelper.setParentKey(parentKey);
    }
}
