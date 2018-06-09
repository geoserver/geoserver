/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.LegacyServiceLoader;
import org.geoserver.config.util.LegacyServicesReader;
import org.geoserver.wfs.GMLInfo.SrsNameStyle;
import org.geotools.util.Version;

public class WFSLoader extends LegacyServiceLoader<WFSInfo> {

    public Class<WFSInfo> getServiceClass() {
        return WFSInfo.class;
    }

    public WFSInfo load(LegacyServicesReader reader, GeoServer geoServer) throws Exception {

        WFSInfoImpl wfs = new WFSInfoImpl();
        wfs.setId("wfs");

        Map<String, Object> properties = reader.wfs();
        readCommon(wfs, properties, geoServer);

        // service level
        wfs.setServiceLevel(WFSInfo.ServiceLevel.get((Integer) properties.get("serviceLevel")));

        // max features
        Integer maxFeatures = (Integer) reader.global().get("maxFeatures");
        if (maxFeatures == null) {
            maxFeatures = Integer.MAX_VALUE;
        }
        wfs.setMaxFeatures(maxFeatures);

        Boolean featureBounding = (Boolean) properties.get("featureBounding");
        if (featureBounding != null) {
            wfs.setFeatureBounding(featureBounding);
        }

        Boolean hitsIgnoreMaxFeatures = (Boolean) properties.get("hitsIgnoreMaxFeatures");
        if (hitsIgnoreMaxFeatures != null) {
            wfs.setHitsIgnoreMaxFeatures(hitsIgnoreMaxFeatures);
        }

        // gml2
        GMLInfo gml = new GMLInfoImpl();
        gml.setOverrideGMLAttributes(true);

        Boolean srsXmlStyle = (Boolean) properties.get("srsXmlStyle");
        if (srsXmlStyle) {
            gml.setSrsNameStyle(SrsNameStyle.XML);
        } else {
            gml.setSrsNameStyle(SrsNameStyle.NORMAL);
        }
        wfs.getGML().put(WFSInfo.Version.V_10, gml);

        // gml3
        gml = new GMLInfoImpl();
        gml.setSrsNameStyle(SrsNameStyle.URN);
        gml.setOverrideGMLAttributes(false);
        wfs.getGML().put(WFSInfo.Version.V_11, gml);

        // gml32
        gml = new GMLInfoImpl();
        gml.setSrsNameStyle(SrsNameStyle.URN2);
        gml.setOverrideGMLAttributes(false);
        wfs.getGML().put(WFSInfo.Version.V_20, gml);

        wfs.getVersions().add(new Version("1.0.0"));
        wfs.getVersions().add(new Version("1.1.0"));
        wfs.getVersions().add(new Version("2.0.0"));

        return wfs;
    }
}
