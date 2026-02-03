/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

/** XStreamPersisterInitializer for TemplateLayerConfig class and TemplateRule list. */
public class TemplateLayerConfigXStreamPersisterInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.getXStream().alias("FtRule", TemplateRule.class);
        persister.getXStream().alias("FtTemplateLayerConfig", TemplateLayerConfig.class);
        persister.registerBreifMapComplexType("FtTemplateRuleType", TemplateRule.class);
        persister.registerBreifMapComplexType("FtLayerConfigType", TemplateLayerConfig.class);
    }
}
