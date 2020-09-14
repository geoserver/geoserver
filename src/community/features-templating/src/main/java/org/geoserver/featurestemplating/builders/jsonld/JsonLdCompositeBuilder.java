/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.builders.jsonld;

import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.impl.CompositeBuilder;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.featurestemplating.builders.impl.TemplateBuilderContext;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A CompositeBuilder to produce a Json-LD output. It checks that at least one of its children
 * doesn't evaluate to null before starting the evaluation process.
 */
public class JsonLdCompositeBuilder extends CompositeBuilder {

    public JsonLdCompositeBuilder(String key, NamespaceSupport namespaces) {
        super(key, namespaces);
    }

    @Override
    public boolean canWrite(TemplateBuilderContext context) {
        List<TemplateBuilder> filtered =
                children.stream()
                        .filter(
                                b ->
                                        b instanceof DynamicValueBuilder
                                                || b instanceof CompositeBuilder)
                        .collect(Collectors.toList());
        if (filtered.size() == children.size()) {
            int falseCounter = 0;
            for (TemplateBuilder b : filtered) {
                if (b instanceof CompositeBuilder) {
                    if (!((CompositeBuilder) b).canWrite(context)) falseCounter++;
                } else {
                    if (!((JsonLdDynamicBuilder) b).checkNotNullValue(context)) falseCounter++;
                }
            }
            if (falseCounter == filtered.size()) return false;
        }
        return true;
    }
}
