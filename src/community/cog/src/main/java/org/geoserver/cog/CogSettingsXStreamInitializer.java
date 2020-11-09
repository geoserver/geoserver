/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cog;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

/** Persister initializer registering CogSettings related classes */
public class CogSettingsXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType("cogSettings", CogSettings.class);
        persister.registerBreifMapComplexType("cogSettingsStore", CogSettingsStore.class);
        XStream xs = persister.getXStream();
        xs.alias("cogSettings", CogSettings.class);
        xs.alias("cogSettingsStore", CogSettingsStore.class);
    }
}
