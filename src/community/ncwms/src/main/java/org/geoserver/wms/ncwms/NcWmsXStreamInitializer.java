/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

public class NcWmsXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType("ncwms", NcWmsInfo.class);
        XStream xs = persister.getXStream();
        xs.allowTypes(new Class[] {NcWmsInfo.class, NcWMSInfoImpl.class});
        xs.addDefaultImplementation(NcWMSInfoImpl.class, NcWmsInfo.class);
    }
}
