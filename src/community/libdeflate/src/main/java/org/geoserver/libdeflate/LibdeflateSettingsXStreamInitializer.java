/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.libdeflate;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

/** Persister initializer registering LibdeflateSettings related classes */
public class LibdeflateSettingsXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType("libdeflateSettings", LibdeflateSettings.class);
        XStream xs = persister.getXStream();
        xs.alias("libdeflateSettings", LibdeflateSettings.class);
    }
}
