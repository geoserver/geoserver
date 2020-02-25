/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.web.netcdf;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geoserver.web.netcdf.NetCDFSettingsContainer.ExtraVariable;

public class ExtraVariableXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBreifMapComplexType("extraVariable", ExtraVariable.class);
        XStream xs = persister.getXStream();
        xs.alias("extraVariable", ExtraVariable.class);
    }
}
