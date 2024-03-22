/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import org.geotools.api.filter.Filter;

/** A class to represent a MapML style */
public class MapMLStyle {
    public static final String RULE_ID_PREFIX = "r";

    public static final String NAME_DELIMITER = "-";
    public static final String SYMBOLIZER_ID_PREFIX = "s";

    private String styleId;
    private int ruleId;
    private int symbolizerId;
    private String symbolizerType;

    private Map<String, String> properties = new HashMap<>();

    private boolean isElseFilter = false;

    private Filter filter;

    public String getStyleId() {
        return styleId;
    }

    public void setStyleId(String styleId) {
        this.styleId = styleId;
    }

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
        StringJoiner joiner = new StringJoiner("; ");
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            joiner.add(entry.getKey() + ":" + entry.getValue());
        }
        return joiner.toString();
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
        StringJoiner joiner = new StringJoiner(NAME_DELIMITER);
        if (styleId != null) joiner.add(styleId);
        joiner.add(RULE_ID_PREFIX + ruleId);
        joiner.add(SYMBOLIZER_ID_PREFIX + symbolizerId);
        return joiner.toString();
    }
}
