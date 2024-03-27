/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Contains the configuration the setting Content-Security-Policy response header. */
public class CSPConfiguration implements Serializable {

    private static final long serialVersionUID = 975607551448404268L;

    /** Whether the Content-Security-Policy header is enabled */
    private Boolean enabled;

    /** Whether to inject the proxy base URL into fetch directives */
    private Boolean injectProxyBase;

    /** External hosts to inject into fetch directives for static web files */
    private String externalResources;

    /** External hosts to add to the frame-ancestors directive */
    private String frameAncestors;

    /** The policies for individual Content-Security-Policy headers */
    private List<CSPPolicy> policies;

    /** Creates a new CSPConfiguration object with default values. */
    public CSPConfiguration() {
        this(true, false, "", "", new ArrayList<>());
    }

    /**
     * Creates a new CSPConfiguration object with the provided values.
     *
     * @param enabled whether the Content-Security-Policy header is enabled
     * @param injectProxyBase whether to inject the proxy base URL into fetch directives
     * @param externalResources external hosts to inject into fetch directives for static web files
     * @param frameAncestors external hosts to add to the frame-ancestors directive
     * @param policies the policies for individual Content-Security-Policy headers
     */
    public CSPConfiguration(
            boolean enabled,
            boolean injectProxyBase,
            String externalResources,
            String frameAncestors,
            List<CSPPolicy> policies) {
        this.enabled = enabled;
        this.injectProxyBase = injectProxyBase;
        this.externalResources = externalResources;
        this.frameAncestors = frameAncestors;
        this.policies = policies;
    }

    /**
     * Creates a copy of the provided CSPConfiguration object.
     *
     * @param other the configuration to copy
     */
    public CSPConfiguration(CSPConfiguration other) {
        this(
                other.isEnabled(),
                other.isInjectProxyBase(),
                other.getExternalResources(),
                other.getFrameAncestors(),
                other.getPolicies().stream().map(CSPPolicy::new).collect(Collectors.toList()));
    }

    /** @return whether the Content-Security-Policy header is enabled */
    public boolean isEnabled() {
        return this.enabled;
    }

    /** @param enabled whether the Content-Security-Policy header is enabled */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return whether to inject the proxy base URL into fetch directives */
    public boolean isInjectProxyBase() {
        return this.injectProxyBase;
    }

    /** @param injectProxyBase whether to inject the proxy base URL into fetch directives */
    public void setInjectProxyBase(boolean injectProxyBase) {
        this.injectProxyBase = injectProxyBase;
    }

    /** @return external hosts to inject into fetch directives for static web files */
    public String getExternalResources() {
        return this.externalResources;
    }

    /**
     * @param externalResources external hosts to inject into fetch directives for static web files
     */
    public void setExternalResources(String externalResources) {
        this.externalResources = externalResources;
    }

    /** @return external hosts to add to the frame-ancestors directive */
    public String getFrameAncestors() {
        return this.frameAncestors;
    }

    /** @param frameAncestors external hosts to add to the frame-ancestors directive */
    public void setFrameAncestors(String frameAncestors) {
        this.frameAncestors = frameAncestors;
    }

    /** @return the policies for individual Content-Security-Policy headers */
    public List<CSPPolicy> getPolicies() {
        return this.policies;
    }

    /** @param policies the policies for individual Content-Security-Policy headers */
    public void setPolicies(List<CSPPolicy> policies) {
        this.policies = policies;
    }

    /**
     * Gets the value of the field that matches the specific key, trimming leading and trailing
     * whitespace from the value and returns that value or an empty string if no field matches the
     * key.
     *
     * @param key the field key
     * @return the field value or an empty string
     */
    public String getField(String key) {
        switch (key) {
            case CSPUtils.GEOSERVER_CSP_EXTERNAL_RESOURCES:
                return CSPUtils.trimWhitespace(this.externalResources);
            case CSPUtils.GEOSERVER_CSP_FRAME_ANCESTORS:
                return CSPUtils.trimWhitespace(this.frameAncestors);
            default:
                return "";
        }
    }

    /**
     * Gets the policy with the specified name.
     *
     * @param name the policy name
     * @return the policy of null
     */
    public CSPPolicy getPolicyByName(String name) {
        return this.policies.stream()
                .filter(p -> name.equals(p.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Parses the filter string for all of the rules in this configuration.
     *
     * @return this configuration object
     */
    public CSPConfiguration parseFilters() {
        getPolicies().stream()
                .map(CSPPolicy::getRules)
                .flatMap(List::stream)
                .forEach(CSPRule::parseFilter);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CSPConfiguration) {
            CSPConfiguration other = (CSPConfiguration) obj;
            return Objects.equals(this.enabled, other.enabled)
                    && Objects.equals(this.injectProxyBase, other.injectProxyBase)
                    && Objects.equals(this.externalResources, other.externalResources)
                    && Objects.equals(this.frameAncestors, other.frameAncestors)
                    && Objects.equals(this.policies, other.policies);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.enabled,
                this.injectProxyBase,
                this.externalResources,
                this.frameAncestors,
                this.policies);
    }

    /** Initialize after XStream deserialization */
    private Object readResolve() {
        if (this.enabled == null) {
            this.enabled = true;
        }
        if (this.injectProxyBase == null) {
            this.injectProxyBase = false;
        }
        if (this.externalResources == null) {
            this.externalResources = "";
        }
        if (this.frameAncestors == null) {
            this.frameAncestors = "";
        }
        if (this.policies == null) {
            this.policies = new ArrayList<>();
        } else {
            Set<String> names = new HashSet<>();
            Preconditions.checkArgument(
                    this.policies.stream().map(CSPPolicy::getName).allMatch(names::add),
                    "Policy names must be unique");
        }
        return parseFilters();
    }
}
