/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

/** XStreamPersisterInitializer for TemplateLayerConfig class and TemplateRule list. */
public class SchemaLayerConfigXStreamPersisterInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.getXStream().alias("SchemaRule", SchemaRule.class);
        persister.getXStream().alias("SchemaLayerConfig", SchemaLayerConfig.class);
        persister.registerBreifMapComplexType("SchemaRuleType", SchemaRule.class);
        persister.registerBreifMapComplexType("SchemaLayerConfigType", SchemaLayerConfig.class);
    }
}
