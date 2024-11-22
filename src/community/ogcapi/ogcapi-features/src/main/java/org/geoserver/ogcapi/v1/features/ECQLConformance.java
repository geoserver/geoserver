package org.geoserver.ogcapi.v1.features;

import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.wfs.WFSInfo;

import static org.geoserver.ogcapi.APIConformance.Level.COMMUNITY_STANDARD;

/**
 * ECQL Configuration for FeatureService.
 */
public class ECQLConformance extends ConformanceInfo<WFSInfo> {

    // informal
    public static final APIConformance ECQL = new APIConformance(ConformanceClass.ECQL, COMMUNITY_STANDARD);
    public static final APIConformance ECQL_TEXT = new APIConformance(ConformanceClass.ECQL_TEXT, COMMUNITY_STANDARD);

    public ECQLConformance(WFSInfo service) {
        super("ecql",ECQL,service);
    }

    public boolean isECQL() {
        return isEnabled(ECQL);
    }
    public void setECQL(boolean enabled) {
        setEnabled(ECQL, enabled);
    }

    public boolean isText() {
        return isEnabled("text", ECQL_TEXT);
    }
    public void setText(boolean enabled) {
        setEnabled("text", enabled);
    }

}