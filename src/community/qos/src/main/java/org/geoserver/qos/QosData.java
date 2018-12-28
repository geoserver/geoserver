/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos;

import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.qos.xml.OperationalStatus;
import org.geoserver.wms.GetMapOutputFormat;

public class QosData {

    protected static volatile QosData INSTANCE;

    public static QosData instance() {
        if (INSTANCE == null) {
            synchronized (QosData.class) {
                if (INSTANCE == null) INSTANCE = new QosData();
            }
        }
        return INSTANCE;
    }

    private QosData() {}

    public List<String> getOperationalStatusList() {
        String url = OperationalStatus.URL;
        List<String> list =
                OperationalStatus.valuesStringList()
                        .stream()
                        .map(x -> url.concat(x))
                        .collect(Collectors.toList());
        return list;
    }

    public List<String> getWmsOutputFormats() {
        // get beans of type GetMapOutputFormat
        List<GetMapOutputFormat> formats = GeoServerExtensions.extensions(GetMapOutputFormat.class);
        return formats.stream().map(f -> f.getMimeType()).collect(Collectors.toList());
    }
}
