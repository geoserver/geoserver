/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext.config;

import java.io.Serializable;

/** A rule for the {@link org.geoserver.proxybase.ext.ProxyBaseExtUrlMangler}. */
public class ProxyBaseExtensionRule implements Serializable {
    private String id;
    private String matcher;
    private String transformer;
    private boolean activated = true;
    private Integer position;

    /**
     * Populating constructor.
     *
     * @param id the id of the rule
     * @param matcher the matcher
     * @param transformer the transformer
     * @param activated if the rule is activated
     * @param position the position of the rule in processing order
     */
    public ProxyBaseExtensionRule(
            String id, String matcher, String transformer, boolean activated, int position) {
        this.id = id;
        this.matcher = matcher;
        this.transformer = transformer;
        this.activated = activated;
        this.position = position;
    }

    /** Default constructor. */
    public ProxyBaseExtensionRule() {
        // default constructor
    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public String getTransformer() {
        return transformer;
    }

    public void setTransformer(String transformer) {
        this.transformer = transformer;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    /**
     * Compares the position of the rule with the position of the given rule.
     *
     * @param proxyBaseExtensionRule the rule to compare with
     * @return the result of the comparison
     */
    public int compareTo(ProxyBaseExtensionRule proxyBaseExtensionRule) {
        if (this.getPosition() == null || proxyBaseExtensionRule.getPosition() == null) {
            return 0;
        }
        return this.position - proxyBaseExtensionRule.position;
    }
}
