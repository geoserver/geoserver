/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.springframework.stereotype.Component;

/** Configures XStream so the {@link MapsConformance} configuration persists in the WMSInfo metadata map. */
@Component
public class MapsServiceXStreamPersisterInitializer implements XStreamPersisterInitializer {
    @Override
    public void init(XStreamPersister persister) {
        persister.registerBriefMapComplexType(MapsConformance.METADATA_KEY, MapsConformance.class);
        XStream xs = persister.getXStream();
        xs.allowTypes(new Class[] {MapsConformance.class});
    }
}
