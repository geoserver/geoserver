/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.util.HashMap;
import java.util.Map;
import org.geotools.api.filter.Filter;

/** A class to represent a MapML style */
public class MapMLStyle {
    public static final String RULE_ID_PREFIX = "rule-";

    public static final String NAME_DELIMITER = "_";
    public static final String SYMBOLIZER_ID_PREFIX = "symbolizer-";
    private int ruleId;
    private int symbolizerId;
    private String symbolizerType;

    private Map<String, String> properties = new HashMap<>();

    private boolean isElseFilter = false;

    private Filter filter;

    /**
     * Set the rule ID
     *
     * @param ruleId the rule ID
     */
    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    /**
     * Get the rule ID
     *
     * @return the rule ID
     */
    public int getRuleId() {
        return ruleId;
    }

    /**
     * Get the symbolizer ID
     *
     * @return the symbolizer ID
     */
    public int getSymbolizerId() {
        return symbolizerId;
    }

    /**
     * Set the symbolizer ID
     *
     * @param symbolizerId the symbolizer ID
     */
    public void setSymbolizerId(int symbolizerId) {
        this.symbolizerId = symbolizerId;
    }

    /**
     * Get the symbolizer type
     *
     * @return the symbolizer type
     */
    public String getSymbolizerType() {
        return symbolizerType;
    }

    /**
     * Set the symbolizer type
     *
     * @param symbolizerType the symbolizer type
     */
    public void setSymbolizerType(String symbolizerType) {
        this.symbolizerType = symbolizerType;
    }

    /**
     * Set a style key value pair
     *
     * @param key the key
     * @param value the value
     */
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * Get a style property
     *
     * @param key the key
     * @return the value
     */
    public String getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Get the style properties
     *
     * @return the style properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * If this style is an else filter
     *
     * @return true if this style is an else filter
     */
    public boolean isElseFilter() {
        return isElseFilter;
    }

    /**
     * Set if this style is an else filter
     *
     * @param isElseFilter true if this style is an else filter
     */
    public void setElseFilter(boolean isElseFilter) {
        this.isElseFilter = isElseFilter;
    }

    /**
     * Get the filter
     *
     * @return the filter
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Set the filter
     *
     * @param filter the filter
     */
    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    /**
     * Get the properties as a CSS style string
     *
     * @return the properties as a CSS style string
     */
    public String getPropertiesAsCSS() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(";");
        }
        return sb.toString();
    }

    /**
     * Get the style as a CSS style string
     *
     * @return the style as a CSS style string
     */
    public String getStyleAsCSS() {
        return getCSSClassName() + "{" + getPropertiesAsCSS() + "}";
    }

    /**
     * Get the CSS class name for this style
     *
     * @return the CSS class name for this style
     */
    public String getCSSClassName() {
        return RULE_ID_PREFIX + ruleId + NAME_DELIMITER + SYMBOLIZER_ID_PREFIX + symbolizerId;
    }
}
