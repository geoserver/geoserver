/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;
import org.geoserver.web.netcdf.layer.NetCDFLayerSettingsContainer;

public class NetCDFContainerXStreamInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.registerBriefMapComplexType("netcdfSettingsContainer", NetCDFSettingsContainer.class);
        persister.registerBriefMapComplexType("netcdfLayerSettingsContainer", NetCDFLayerSettingsContainer.class);
        XStream xs = persister.getXStream();
        xs.alias("netCDFSettings", NetCDFSettingsContainer.class);
        xs.alias("netCDFLayerSettings", NetCDFLayerSettingsContainer.class);
        // Alias for the per-band output settings list element introduced for multi-band coverages —
        // see NetCDFSettingsContainer.BandSetting javadoc. Without an alias the persisted XML would
        // carry the fully qualified inner class name as the element tag.
        xs.alias("bandSetting", NetCDFSettingsContainer.BandSetting.class);
        xs.allowTypes(new Class[] {NetCDFSettingsContainer.BandSetting.class});
    }
}
