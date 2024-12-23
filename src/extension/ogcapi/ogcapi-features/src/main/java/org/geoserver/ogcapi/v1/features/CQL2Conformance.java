/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.geoserver.ogcapi.APIConformance.Level.DRAFT_STANDARD;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.ogcapi.APIConformance;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.ConformanceInfo;
import org.geoserver.wfs.WFSInfo;

/** CQL2 Configuration for FeatureService. */
public class CQL2Conformance extends ConformanceInfo<WFSInfo> {
    public static final String METADATA_KEY = "cql2";
    /** CQL Text conformance. */
    public static final APIConformance CQL2_TEXT = new APIConformance(ConformanceClass.CQL2_TEXT);

    /** CQL JSON conformance - not implemented yet (very different from the binding we have) */
    public static final APIConformance CQL2_JSON = new APIConformance(ConformanceClass.CQL2_JSON, DRAFT_STANDARD);

    // CQL is optional
    public static final APIConformance CQL2_ADVANCED = new APIConformance(ConformanceClass.CQL2_ADVANCED);
    public static final APIConformance CQL2_ARITHMETIC = new APIConformance(ConformanceClass.CQL2_ARITHMETIC);
    public static final APIConformance CQL2_BASIC = new APIConformance(ConformanceClass.CQL2_BASIC);
    public static final APIConformance CQL2_BASIC_SPATIAL = new APIConformance(ConformanceClass.CQL2_BASIC_SPATIAL);

    /**
     * Indicates CQL2 Functions are supported.
     *
     * <p>FeatureService is required to support {@code /functions} endpoint, providing a
     * {@link org.geoserver.ogcapi.FunctionsDocument}
     */
    public static final APIConformance CQL2_FUNCTIONS = new APIConformance(ConformanceClass.CQL2_FUNCTIONS);

    /** CQL2_TEMPORAL excluded for now, no support for all operators. */
    public static final APIConformance CQL2_TEMPORAL = new APIConformance(ConformanceClass.CQL2_TEMPORAL);

    /** CQL2_ARRAY excluded, no support for array operations now. */
    public static final APIConformance CQL2_ARRAY =
            new APIConformance("http://www.opengis.net/spec/ogcapi-features-3/1.0/req/arrays");

    public static final APIConformance CQL2_PROPERTY_PROPERTY =
            new APIConformance(ConformanceClass.CQL2_PROPERTY_PROPERTY);
    public static final APIConformance CQL2_SPATIAL = new APIConformance(ConformanceClass.CQL2_SPATIAL);

    // CQL2 formats
    private Boolean json;
    private Boolean text;

    // CQL functionality
    private Boolean advanced;
    private Boolean arithmetic;
    private Boolean basic;
    private Boolean basicSpatial;
    private Boolean functions;
    private Boolean propertyProperty;
    private Boolean spatial;

    public CQL2Conformance() {}

    /**
     * Obtain CQL2Conformance configuration for WFSInfo.
     *
     * <p>Uses configuration stored in metadata map, or creates default if needed.
     *
     * @param wfsInfo WFSService configuration
     * @return CQL2 configuration
     */
    public static CQL2Conformance configuration(WFSInfo wfsInfo) {
        if (wfsInfo.getMetadata().containsKey(METADATA_KEY)) {
            return (CQL2Conformance) wfsInfo.getMetadata().get(METADATA_KEY);
        } else {
            CQL2Conformance conf = new CQL2Conformance();
            wfsInfo.getMetadata().put(METADATA_KEY, conf);
            return conf;
        }
    }

    /**
     * Enable for either CQL2_TEXT or CQL2_JSON enabled.
     *
     * @return Enable for either CQL2_TEXT or CQL2_JSON
     */
    @Override
    public boolean isEnabled(WFSInfo info) {
        return text(info) || json(info);
    }

    @Override
    public List<APIConformance> conformances(WFSInfo wfsInfo) {
        List<APIConformance> conformanceList = new ArrayList<>();
        if (isEnabled(wfsInfo)) {
            if (text(wfsInfo)) {
                conformanceList.add(CQL2_TEXT);
            }
            if (json(wfsInfo)) {
                conformanceList.add(CQL2_JSON);
            }

            if (basic(wfsInfo)) {
                conformanceList.add(CQL2_BASIC);
            }
            if (advanced(wfsInfo)) {
                conformanceList.add(CQL2_ADVANCED);
            }
            if (arithmetic(wfsInfo)) {
                conformanceList.add(CQL2_ARITHMETIC);
            }
            if (propertyProperty(wfsInfo)) {
                conformanceList.add(CQL2_PROPERTY_PROPERTY);
            }
            if (basicSpatial(wfsInfo)) {
                conformanceList.add(CQL2_BASIC_SPATIAL);
            }
            if (spatial(wfsInfo)) {
                conformanceList.add(CQL2_SPATIAL);
            }
            if (functions(wfsInfo)) {
                conformanceList.add(CQL2_FUNCTIONS);
            }
        }
        return conformanceList;
    }

    public Boolean isText() {
        return text;
    }

    public void setText(Boolean enabled) {
        text = enabled;
    }

    public boolean text(WFSInfo info) {
        return isEnabled(info, text, CQL2_TEXT);
    }

    public Boolean isJSON() {
        return json;
    }

    public void setJSON(Boolean enabled) {
        json = enabled;
    }

    public boolean json(WFSInfo info) {
        return isEnabled(info, json, CQL2_JSON);
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public void setCql2Advanced(boolean enabled) {
        advanced = enabled;
    }

    public boolean advanced(WFSInfo info) {
        return isEnabled(info, advanced, CQL2_ADVANCED);
    }

    public Boolean isArithmetic() {
        return arithmetic;
    }

    public void setArtihmetic(Boolean enabled) {
        arithmetic = enabled;
    }

    public boolean arithmetic(WFSInfo info) {
        return isEnabled(info, arithmetic, CQL2_ARITHMETIC);
    }

    public Boolean isBasic() {
        return basic;
    }

    public void setBasic(Boolean enabled) {
        basic = enabled;
    }

    public boolean basic(WFSInfo info) {
        return isEnabled(info, basic, CQL2_BASIC);
    }

    public Boolean isBasicSpatial() {
        return basicSpatial;
    }

    public void setBasicSpatial(Boolean enabled) {
        basicSpatial = enabled;
    }

    public boolean basicSpatial(WFSInfo info) {
        return isEnabled(info, basicSpatial, CQL2_BASIC_SPATIAL);
    }

    public Boolean isFunctions() {
        return functions;
    }

    public void setFunctions(Boolean enabled) {
        functions = enabled;
    }

    public boolean functions(WFSInfo info) {
        return isEnabled(info, functions, CQL2_FUNCTIONS);
    }

    public Boolean isPropertyProperty() {
        return propertyProperty;
    }

    public void setPropertyProperty(Boolean enabled) {
        propertyProperty = enabled;
    }

    public boolean propertyProperty(WFSInfo info) {
        return isEnabled(info, propertyProperty, CQL2_PROPERTY_PROPERTY);
    }

    public Boolean isSpatial() {
        return spatial;
    }

    public void setSpatial(Boolean enabled) {
        spatial = enabled;
    }

    public boolean spatial(WFSInfo info) {
        return isEnabled(info, spatial, CQL2_SPATIAL);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CQL2Conformance");
        sb.append(" ").append(METADATA_KEY);
        sb.append("{ text=").append(text);
        sb.append("{ json=").append(json);
        sb.append('}');
        return sb.toString();
    }
}
