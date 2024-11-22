package org.geoserver.ogcapi.v1.features;

import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.wfs.WFSInfo;

import java.util.ArrayList;
import java.util.List;

import static org.geoserver.ogcapi.APIConformance.Level.COMMUNITY_STANDARD;
import static org.geoserver.ogcapi.APIConformance.Level.DRAFT_STANDARD;

/**
 * CQL2 Configuration for FeatureService.
 */
public class CQL2Conformance extends ConformanceInfo<WFSInfo> {

    /**
     * CQL Text conformance.
     */
    public static final APIConformance CQL2_TEXT = new APIConformance(ConformanceClass.CQL2_TEXT);

    /**
     * CQL JSON conformance - not implemented yet (very different from the binding we have)
     */
    public static final APIConformance CQL2_JSON = new APIConformance(ConformanceClass.CQL2_JSON, DRAFT_STANDARD);

    // CQL is optional
    public static final APIConformance CQL2_ADVANCED = new APIConformance(ConformanceClass.CQL2_ADVANCED);
    public static final APIConformance CQL2_ARITHMETIC = new APIConformance(ConformanceClass.CQL2_ARITHMETIC);
    public static final APIConformance CQL2_BASIC = new APIConformance(ConformanceClass.CQL2_BASIC);
    public static final APIConformance CQL2_BASIC_SPATIAL = new APIConformance(ConformanceClass.CQL2_BASIC_SPATIAL);
    public static final APIConformance CQL2_FUNCTIONS = new APIConformance(ConformanceClass.CQL2_FUNCTIONS);

    /**
     * CQL2_TEMPORAL excluded for now, no support for all operators.
     */
    public static final APIConformance CQL2_TEMPORAL = new APIConformance(ConformanceClass.CQL2_TEMPORAL);

    /**
     * CQL2_ARRAY excluded, no support for array operations now.
     */
    public static final APIConformance CQL2_ARRAY = new APIConformance("http://www.opengis.net/spec/ogcapi-features-3/1.0/req/arrays");

    public static final APIConformance CQL2_PROPERTY_PROPERTY = new APIConformance(ConformanceClass.CQL2_PROPERTY_PROPERTY);
    public static final APIConformance CQL2_SPATIAL = new APIConformance(ConformanceClass.CQL2_SPATIAL);

    public CQL2Conformance(WFSInfo service) {
        super(CQL2_TEXT,service);
    }

    List<APIConformance> getConformances() {
        List<APIConformance> conformances = new ArrayList<>();
        if (isEnabled()) {
            if (isBasic()) {
                conformances.add(CQL2_BASIC);
            }
            if (isAdvanced()) {
                conformances.add(CQL2_ADVANCED);
            }
            if (isArithmetic()) {
                conformances.add(CQL2_ARITHMETIC);
            }
            if (isPropertyProperty()) {
                conformances.add(CQL2_PROPERTY_PROPERTY);
            }
            if (isBasicSpatial()) {
                conformances.add(CQL2_BASIC_SPATIAL);
            }
            if (isSpatial()) {
                conformances.add(CQL2_SPATIAL);
            }
            if (isFunctions()) {
                conformances.add(CQL2_FUNCTIONS);
            }
            conformances.add(CQL2_TEXT);
            if (isJSON()) {
                conformances.add(CQL2_JSON);
            }
        }
        return conformances;
    }

    public boolean isJSON() {
        return isEnabled("json", CQL2_JSON);
    }
    public void setJSON(boolean enabled) {
        setEnabled("json", enabled);
    }

    public boolean isAdvanced() {
        return isEnabled("advanced", CQL2_ADVANCED);
    }
    public void setCql2Advanced(boolean enabled) {
        setEnabled("advanced", enabled);
    }

    public boolean isArithmetic() {
        return isEnabled("arithmetic", CQL2_ARITHMETIC);
    }
    public void setArtihmetic(boolean enabled) {
        setEnabled("arithmetic", enabled);
    }

    public boolean isBasic() {
        return isEnabled("basic", CQL2_BASIC);
    }
    public void setBasic(boolean enabled) {
        setEnabled("basic", enabled);
    }

    public boolean isBasicSpatial() {
        return isEnabled("basic_spatial", CQL2_BASIC_SPATIAL);
    }
    public void setBasicSpatial(boolean enabled) {
        setEnabled("basic_spatial", enabled);
    }

    public boolean isFunctions() {
        return isEnabled("functions", CQL2_FUNCTIONS);
    }

    public void setFunctions(boolean enabled) {
        setEnabled("functions", enabled);
    }

    public boolean isPropertyProperty() {
        return isEnabled("property_property", CQL2_PROPERTY_PROPERTY);
    }
    public void setPropertyProperty(boolean enabled) {
        setEnabled("property_property", enabled);
    }
    public boolean isSpatial() {
        return isEnabled("spatial", CQL2_SPATIAL);
    }
    public void setSpatial(boolean enabled) {
        setEnabled("spatial", enabled);
    }
}