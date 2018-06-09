package org.geoserver.jdbcconfig.internal;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

public class JDBCConfigXStreamPersisterInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister
                .getXStream()
                .allowTypes(
                        new String[] {
                            "org.geoserver.wfs.WFSInfo.Version",
                            "org.geoserver.wfs.WFSInfo$Version",
                            "org.geoserver.wms.WatermarkInfoImpl",
                            "org.geoserver.wfs.GMLInfoImpl"
                        });
    }
}
