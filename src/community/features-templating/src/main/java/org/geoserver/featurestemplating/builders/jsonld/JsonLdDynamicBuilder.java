/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.jsonld;

import java.util.List;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.geotools.feature.ComplexAttributeImpl;
import org.opengis.feature.Attribute;
import org.opengis.feature.ComplexAttribute;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A DynamicBuilder used to encode Json-LD output. It check that the value being encoded is not null
 * before writing.
 */
public class JsonLdDynamicBuilder extends DynamicValueBuilder {

    public JsonLdDynamicBuilder(String key, String expression, NamespaceSupport namespaces) {
        super(key, expression, namespaces);
    }

    @Override
    protected boolean canWriteValue(Object value) {
        if (value instanceof ComplexAttributeImpl) {
            return canWriteValue(((ComplexAttribute) value).getValue());
        } else if (value instanceof Attribute) {
            return canWriteValue(((Attribute) value).getValue());
        } else if (value instanceof List && ((List) value).size() == 0) {
            if (((List) value).size() == 0) return false;
            else return true;
        } else if (value == null) {
            return false;
        } else {
            return true;
        }
    }

    public boolean checkNotNullValue(TemplateBuilderContext context) {
        Object o = null;
        if (xpath != null) {

            o = evaluateXPath(context);

        } else if (cql != null) {
            o = evaluateExpressions(context);
        }
        if (o == null) return false;
        return true;
    }
}
