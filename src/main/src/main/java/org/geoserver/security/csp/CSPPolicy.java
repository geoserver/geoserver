/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.util.logging.Logging;

/** Contains the rules to determine the value for each Content Security Policy header. */
public class CSPPolicy implements Serializable {

    private static final long serialVersionUID = -6131742124949554000L;

    /** The CSPPolicy class logger. */
    private static final Logger LOGGER = Logging.getLogger(CSPPolicy.class);

    // default values
    private static final String DEFAULT_NAME = null;
    private static final String DEFAULT_DESCRIPTION = "";
    private static final Boolean DEFAULT_ENABLED = true;

    /** The policy name */
    private String name;

    /** The policy description */
    private String description;

    /** Whether the policy is enabled */
    private Boolean enabled;

    /** The rules */
    private List<CSPRule> rules;

    /** Creates a new CSPPolicy object with default values. */
    public CSPPolicy() {
        this(DEFAULT_NAME, DEFAULT_DESCRIPTION, DEFAULT_ENABLED, new ArrayList<>());
    }

    /**
     * Creates a new CSPPolicy object with the provided values.
     *
     * @param name the policy name
     * @param description the policy description
     * @param enabled whether the policy is enabled
     * @param rules the rules
     */
    public CSPPolicy(String name, String description, boolean enabled, List<CSPRule> rules) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.rules = rules;
    }

    /**
     * Creates a copy of the provided CSPPolicy object.
     *
     * @param other the policy to copy
     */
    public CSPPolicy(CSPPolicy other) {
        this(
                other.getName(),
                other.getDescription(),
                other.isEnabled(),
                other.getRules().stream().map(CSPRule::new).collect(Collectors.toList()));
    }

    /** @return the policy name */
    public String getName() {
        return this.name;
    }

    /** @param name the policy name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the policy description */
    public String getDescription() {
        return this.description;
    }

    /** @param description the policy description */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return whether the policy is enabled */
    public boolean isEnabled() {
        return this.enabled;
    }

    /** @param enabled whether the policy is enabled */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return the rules */
    public List<CSPRule> getRules() {
        return this.rules;
    }

    /** @param rules the rules */
    public void setRules(List<CSPRule> rules) {
        this.rules = rules;
    }

    /**
     * Tests the rules for this policy against the request until the first matching rule is found. If the matching rules
     * does not contain any directives, checks the previous rules until one is found with directives. Returns null if
     * this policy is disabled, no rule matches the request, the matching rule and all previous rules do not contain
     * directives, or the special directives keyword NONE is found.
     *
     * @param request the HTTP request
     * @return the Content-Security-Policy directives or null
     */
    public String getDirectives(CSPHttpRequestWrapper request) {
        if (this.enabled) {
            for (int i = 0; i < this.rules.size(); i++) {
                CSPRule rule = this.rules.get(i);
                if (rule.test(request)) {
                    String directives = null;
                    for (int j = i; j >= 0 && directives == null; j--) {
                        directives = this.rules.get(j).getDirectives();
                        directives = Strings.emptyToNull(CSPUtils.trimWhitespace(directives));
                    }
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.finer("Matched " + rule.getName() + ": " + directives);
                    }
                    if (directives == null || "NONE".equalsIgnoreCase(directives)) {
                        return null;
                    }
                    return CSPUtils.cleanDirectives(directives);
                }
            }
        }
        return null;
    }

    /**
     * Gets the rule with the specified name.
     *
     * @param name the rule name
     * @return the rule of null
     */
    public CSPRule getRuleByName(String name) {
        return this.rules.stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CSPPolicy) {
            CSPPolicy other = (CSPPolicy) obj;
            return Objects.equals(this.name, other.name)
                    && Objects.equals(this.description, other.description)
                    && Objects.equals(this.enabled, other.enabled)
                    && Objects.equals(this.rules, other.rules);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.description, this.enabled, this.rules);
    }

    /** Initialize after XStream deserialization */
    private Object readResolve() {
        Preconditions.checkNotNull(this.name, "The policy name can not be null");
        this.description = firstNonNull(this.description, DEFAULT_DESCRIPTION);
        this.enabled = firstNonNull(this.enabled, DEFAULT_ENABLED);
        this.rules = firstNonNull(this.rules, new ArrayList<>());
        Preconditions.checkArgument(
                this.rules.stream().map(CSPRule::getName).distinct().count() == this.rules.size(),
                "Rule names must be unique with a policy");
        return this;
    }
}
