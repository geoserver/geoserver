/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import java.util.List;
import java.util.Optional;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.wms.WMSInfo;

public class NcWmsConfigurationListener extends ConfigurationListenerAdapter {

    private final NcWmsService ncwms;

    public NcWmsConfigurationListener(GeoServer gs, NcWmsService ncwms) {
        gs.addListener(this);
        this.ncwms = ncwms;
    }

    @Override
    public void handleServiceChange(
            ServiceInfo service,
            List<String> propertyNames,
            List<Object> oldValues,
            List<Object> newValues) {
        if (service instanceof WMSInfo
                && propertyNames.contains("metadata")
                && configChanged(propertyNames.indexOf("metadata"), oldValues, newValues)) {
            ncwms.configurationChanged((WMSInfo) service);
        }
    }

    private boolean configChanged(int idx, List<Object> oldValues, List<Object> newValues) {
        Optional<Integer> oldPoolSize = getPoolSize((MetadataMap) oldValues.get(idx));
        Optional<Integer> newPoolSize = getPoolSize((MetadataMap) newValues.get(idx));

        // let optional handle null comparison
        return !oldPoolSize.equals(newPoolSize);
    }

    private Optional<Integer> getPoolSize(MetadataMap oldMeta) {
        return Optional.ofNullable(oldMeta)
                .map(m -> m.get(NcWmsService.WMS_CONFIG_KEY, NcWmsInfo.class))
                .map(c -> c.getTimeSeriesPoolSize());
    }
}
