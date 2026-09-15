/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geoserver.ogcapi.APIConformance.Level.STANDARD;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.config.ServiceInfo;

/** CQL2 conformance configuration, shared by any OGC API service that supports the filter parameters. */
public class CQL2Conformance extends ConformanceInfo<ServiceInfo> {
    public static final String METADATA_KEY = "cql2";
    /** CQL Text conformance. */
    public static final APIConformance CQL2_TEXT = new APIConformance(ConformanceClass.CQL2_TEXT, STANDARD, "text");

    /** CQL JSON conformance - not implemented yet (very different from the binding we have) */
    public static final APIConformance CQL2_JSON = new APIConformance(ConformanceClass.CQL2_JSON, STANDARD, "json");

    // CQL is optional
    public static final APIConformance CQL2_ADVANCED =
            new APIConformance(ConformanceClass.CQL2_ADVANCED, STANDARD, "advanced");
    public static final APIConformance CQL2_ARITHMETIC =
            new APIConformance(ConformanceClass.CQL2_ARITHMETIC, STANDARD, "arithmetic");
    public static final APIConformance CQL2_BASIC = new APIConformance(ConformanceClass.CQL2_BASIC, STANDARD, "basic");
    public static final APIConformance CQL2_BASIC_SPATIAL =
            new APIConformance(ConformanceClass.CQL2_BASIC_SPATIAL, STANDARD, "basicSpatial");

    /**
     * Indicates CQL2 Functions are supported.
     *
     * <p>The service is required to support {@code /functions} endpoint, providing a
     * {@link org.geoserver.ogcapi.FunctionsDocument}
     */
    public static final APIConformance CQL2_FUNCTIONS =
            new APIConformance(ConformanceClass.CQL2_FUNCTIONS, STANDARD, "functions");

    /** CQL2_TEMPORAL excluded for now, no support for all operators. */
    public static final APIConformance CQL2_TEMPORAL =
            new APIConformance(ConformanceClass.CQL2_TEMPORAL, STANDARD, "temporal");

    /** CQL2_ARRAY excluded, no support for array operations now. */
    public static final APIConformance CQL2_ARRAY =
            new APIConformance("http://www.opengis.net/spec/ogcapi-features-3/1.0/req/arrays", STANDARD, "array");

    public static final APIConformance CQL2_PROPERTY_PROPERTY =
            new APIConformance(ConformanceClass.CQL2_PROPERTY_PROPERTY, STANDARD, "propertyProperty");
    public static final APIConformance CQL2_SPATIAL =
            new APIConformance(ConformanceClass.CQL2_SPATIAL, STANDARD, "spatial");

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

    @Override
    public String getId() {
        return "cql2";
    }

    /**
     * Obtain CQL2Conformance configuration for ServiceInfo.
     *
     * <p>Uses configuration stored in metadata map, or creates default if needed.
     *
     * @param serviceInfo the service configuration holding the metadata map
     * @return CQL2 configuration
     */
    public static CQL2Conformance configuration(ServiceInfo serviceInfo) {
        if (serviceInfo.getMetadata().containsKey(METADATA_KEY)) {
            return (CQL2Conformance) serviceInfo.getMetadata().get(METADATA_KEY);
        } else {
            CQL2Conformance conf = new CQL2Conformance();
            serviceInfo.getMetadata().put(METADATA_KEY, conf);
            return conf;
        }
    }

    /**
     * Enable for either CQL2_TEXT or CQL2_JSON enabled.
     *
     * @return Enable for either CQL2_TEXT or CQL2_JSON
     */
    @Override
    public boolean isEnabled(ServiceInfo info) {
        return text(info) || json(info);
    }

    @Override
    public List<APIConformance> configurableConformances() {
        return List.of(
                CQL2_TEXT,
                CQL2_JSON,
                CQL2_BASIC,
                CQL2_ADVANCED,
                CQL2_ARITHMETIC,
                CQL2_PROPERTY_PROPERTY,
                CQL2_BASIC_SPATIAL,
                CQL2_SPATIAL,
                CQL2_FUNCTIONS);
    }

    @Override
    public List<APIConformance> conformances(ServiceInfo serviceInfo) {
        List<APIConformance> conformanceList = new ArrayList<>();
        if (isEnabled(serviceInfo)) {
            if (text(serviceInfo)) {
                conformanceList.add(CQL2_TEXT);
            }
            if (json(serviceInfo)) {
                conformanceList.add(CQL2_JSON);
            }

            if (basic(serviceInfo)) {
                conformanceList.add(CQL2_BASIC);
            }
            if (advanced(serviceInfo)) {
                conformanceList.add(CQL2_ADVANCED);
            }
            if (arithmetic(serviceInfo)) {
                conformanceList.add(CQL2_ARITHMETIC);
            }
            if (propertyProperty(serviceInfo)) {
                conformanceList.add(CQL2_PROPERTY_PROPERTY);
            }
            if (basicSpatial(serviceInfo)) {
                conformanceList.add(CQL2_BASIC_SPATIAL);
            }
            if (spatial(serviceInfo)) {
                conformanceList.add(CQL2_SPATIAL);
            }
            if (functions(serviceInfo)) {
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

    public boolean text(ServiceInfo info) {
        return isEnabled(info, text, CQL2_TEXT);
    }

    public Boolean isJSON() {
        return json;
    }

    public void setJSON(Boolean enabled) {
        json = enabled;
    }

    public boolean json(ServiceInfo info) {
        return isEnabled(info, json, CQL2_JSON);
    }

    public boolean isAdvanced() {
        return advanced;
    }

    public void setCql2Advanced(boolean enabled) {
        advanced = enabled;
    }

    public boolean advanced(ServiceInfo info) {
        return isEnabled(info, advanced, CQL2_ADVANCED);
    }

    public Boolean isArithmetic() {
        return arithmetic;
    }

    public void setArtihmetic(Boolean enabled) {
        arithmetic = enabled;
    }

    public boolean arithmetic(ServiceInfo info) {
        return isEnabled(info, arithmetic, CQL2_ARITHMETIC);
    }

    public Boolean isBasic() {
        return basic;
    }

    public void setBasic(Boolean enabled) {
        basic = enabled;
    }

    public boolean basic(ServiceInfo info) {
        return isEnabled(info, basic, CQL2_BASIC);
    }

    public Boolean isBasicSpatial() {
        return basicSpatial;
    }

    public void setBasicSpatial(Boolean enabled) {
        basicSpatial = enabled;
    }

    public boolean basicSpatial(ServiceInfo info) {
        return isEnabled(info, basicSpatial, CQL2_BASIC_SPATIAL);
    }

    public Boolean isFunctions() {
        return functions;
    }

    public void setFunctions(Boolean enabled) {
        functions = enabled;
    }

    public boolean functions(ServiceInfo info) {
        return isEnabled(info, functions, CQL2_FUNCTIONS);
    }

    public Boolean isPropertyProperty() {
        return propertyProperty;
    }

    public void setPropertyProperty(Boolean enabled) {
        propertyProperty = enabled;
    }

    public boolean propertyProperty(ServiceInfo info) {
        return isEnabled(info, propertyProperty, CQL2_PROPERTY_PROPERTY);
    }

    public Boolean isSpatial() {
        return spatial;
    }

    public void setSpatial(Boolean enabled) {
        spatial = enabled;
    }

    public boolean spatial(ServiceInfo info) {
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
