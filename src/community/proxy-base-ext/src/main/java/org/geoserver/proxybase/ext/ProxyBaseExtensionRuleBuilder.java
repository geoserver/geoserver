/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext;

import org.geoserver.proxybase.ext.config.ProxyBaseExtRuleDAO;
import org.geoserver.proxybase.ext.config.ProxyBaseExtensionRule;

/** A builder for the {@link ProxyBaseExtensionRule}. */
public class ProxyBaseExtensionRuleBuilder {
    private String id;
    private Boolean activated;
    private String matcher;
    private String transformer;
    private Integer position;

    /**
     * Copy constructor.
     *
     * @param other the rule to copy
     * @return the builder
     */
    public ProxyBaseExtensionRuleBuilder copy(ProxyBaseExtensionRule other) {
        this.id = other.getId();
        this.activated = other.isActivated();
        this.matcher = other.getMatcher();
        this.transformer = other.getTransformer();
        this.position = other.getPosition();
        return this;
    }

    /**
     * Sets the id of the rule.
     *
     * @param id the id
     * @return the builder
     */
    public ProxyBaseExtensionRuleBuilder withId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the activation status of the rule.
     *
     * @param activated the activation status
     * @return the builder
     */
    public ProxyBaseExtensionRuleBuilder withActivated(Boolean activated) {
        this.activated = activated;
        return this;
    }

    /**
     * Sets the matcher of the rule.
     *
     * @param matcher the matcher
     * @return the builder
     */
    public ProxyBaseExtensionRuleBuilder withMatcher(String matcher) {
        this.matcher = matcher;
        return this;
    }

    /**
     * Sets the transformer of the rule.
     *
     * @param transformer the transformer
     * @return the builder
     */
    public ProxyBaseExtensionRuleBuilder withTransformer(String transformer) {
        this.transformer = transformer;
        return this;
    }

    /**
     * Sets the position of the rule.
     *
     * @param position the position
     * @return the builder
     */
    public ProxyBaseExtensionRuleBuilder withPosition(Integer position) {
        if (position != null) {
            this.position = position;
        }
        return this;
    }

    /**
     * Builds the rule.
     *
     * @return the rule
     */
    public ProxyBaseExtensionRule build() {
        checkCondition(id != null, "id is mandatory");
        checkCondition(activated != null, "activated is mandatory");
        checkCondition(matcher != null, "matcher is mandatory");
        checkCondition(transformer != null, "transformer is mandatory");
        checkCondition(position != null, "position is mandatory");
        return new ProxyBaseExtensionRule(id, matcher, transformer, activated, position);
    }

    /**
     * validates rule condition.
     *
     * @param condition the condition
     * @param failMessage the fail message
     * @param failMessageArguments the fail message arguments
     */
    public static void checkCondition(
            boolean condition, String failMessage, Object... failMessageArguments) {
        if (!condition) {
            throw new ProxyBaseExtRuleDAO.ProxyBaseExtException(
                    null, String.format(failMessage, failMessageArguments));
        }
    }
}
