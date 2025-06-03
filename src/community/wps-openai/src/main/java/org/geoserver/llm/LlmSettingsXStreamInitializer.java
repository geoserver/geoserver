/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.llm;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geoserver.llm.model.LlmSettings;

/** XStream Persistence for WPS-OpenAI Settings */
public class LlmSettingsXStreamInitializer implements XStreamPersisterInitializer {
    @Override
    public void init(XStreamPersister persister) {
        persister.registerBriefMapComplexType("llmSettings", LlmSettings.class);
        XStream xs = persister.getXStream();
        xs.alias("llmSettings", LlmSettings.class);
    }
}
