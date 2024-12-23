/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.geoserver.ogcapi.APIConformance.Level.COMMUNITY_STANDARD;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.wfs.WFSInfo;

/** ECQL Configuration for FeatureService. */
public class ECQLConformance extends ConformanceInfo<WFSInfo> {
    public static final String METADATA_KEY = "ecql";

    public static final APIConformance ECQL = new APIConformance(ConformanceClass.ECQL, COMMUNITY_STANDARD);
    public static final APIConformance ECQL_TEXT = new APIConformance(ConformanceClass.ECQL_TEXT, COMMUNITY_STANDARD);

    private Boolean ecql = null;
    private Boolean text = null;

    public ECQLConformance() {}

    @Override
    public boolean isEnabled(WFSInfo serviceInfo) {
        return ecql(serviceInfo);
    }

    /**
     * Obtain FeatureService configuration for WFSInfo.
     *
     * <p>Uses configuration stored in metadata map, or creates default if needed.
     *
     * @param wfsInfo WFSService configuration
     * @return Feature Service configuration
     */
    public static ECQLConformance configuration(WFSInfo wfsInfo) {
        if (wfsInfo.getMetadata().containsKey(METADATA_KEY)) {
            return (ECQLConformance) wfsInfo.getMetadata().get(METADATA_KEY);
        } else {
            ECQLConformance conf = new ECQLConformance();
            wfsInfo.getMetadata().put(METADATA_KEY, conf);
            return conf;
        }
    }

    @Override
    public List<APIConformance> conformances(WFSInfo wfsInfo) {
        List<APIConformance> conformanceList = new ArrayList<>();
        if (isEnabled(wfsInfo)) {
            conformanceList.add(ECQLConformance.ECQL);
            if (text(wfsInfo)) {
                conformanceList.add(ECQLConformance.ECQL_TEXT);
            }
        }
        return conformanceList;
    }

    /**
     * ECQL conformance enabled by configuration.
     *
     * @return Enable ECQL conformance, or @{code null} for default.
     */
    public Boolean isECQL() {
        return ecql;
    }
    /**
     * ECQL conformance enabled by configuration.
     *
     * @return Enable ECQL conformance, or @{code null} for default.
     */
    public boolean ecql(WFSInfo info) {
        return isEnabled(info, ecql, ECQL);
    }
    /**
     * ECQL conformance enablement.
     *
     * @param enabled ECQL conformance enabled, or @{code null} for default.
     */
    public void setECQL(Boolean enabled) {
        ecql = enabled;
    }
    /**
     * ECQL_TEXT conformance enabled by configuration.
     *
     * @return Enable ECQL_TEXT conformance, or @{code null} for default.
     */
    public Boolean isText() {
        return text;
    }
    /**
     * ECQL_TEXT conformance enabled by configuration.
     *
     * @return Enable ECQL_TEXT conformance, or @{code null} for default.
     */
    public boolean text(WFSInfo info) {
        return isEnabled(info, text, ECQL_TEXT);
    }
    /**
     * ECQL_TEXT conformance enablement.
     *
     * @param enabled ECQL_TEXT conformance enabled, or @{code null} for default.
     */
    public void setText(Boolean enabled) {
        text = enabled;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ECQLConformance");
        sb.append(" ").append(METADATA_KEY);
        sb.append("{ ecql=").append(ecql);
        sb.append("{ text=").append(text);
        sb.append('}');
        return sb.toString();
    }
}
