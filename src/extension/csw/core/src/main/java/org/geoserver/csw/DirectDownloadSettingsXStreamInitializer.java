/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

public class DirectDownloadSettingsXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType(
                "directDownloadSettings", DirectDownloadSettings.class);
        XStream xs = persister.getXStream();
        xs.alias("directDownloadSettings", DirectDownloadSettings.class);
    }
}
