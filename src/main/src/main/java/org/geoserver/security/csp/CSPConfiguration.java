/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Contains the configuration the setting Content-Security-Policy response header. */
public class CSPConfiguration implements Serializable {

    private static final long serialVersionUID = 975607551448404268L;

    // default values
    private static final Boolean DEFAULT_ENABLED = true;
    // TODO: can switch to false after all Wicket CSP issues are fixed
    private static final Boolean DEFAULT_REPORT_ONLY = true;
    private static final Boolean DEFAULT_ALLOW_OVERRIDE = false;
    private static final Boolean DEFAULT_INJECT_PROXY_BASE = false;
    private static final String DEFAULT_REMOTE_RESOURCES = "";
    private static final String DEFAULT_FRAME_ANCESTORS = "";

    /** Whether the Content-Security-Policy header is enabled */
    private Boolean enabled;

    /** Whether to report CSP violations without enforcing the policy */
    private Boolean reportOnly;

    /** Whether to allow other components to completely override the CSP */
    private Boolean allowOverride;

    /** Whether to inject the proxy base URL into fetch directives */
    private Boolean injectProxyBase;

    /** Remote hosts to add to fetch directives for static web files */
    private String remoteResources;

    /** Hosts to add to the frame-ancestors directive */
    private String frameAncestors;

    /** The policies for individual Content-Security-Policy headers */
    private List<CSPPolicy> policies;

    /** Creates a new CSPConfiguration object with default values. */
    public CSPConfiguration() {
        this(
                DEFAULT_ENABLED,
                DEFAULT_REPORT_ONLY,
                DEFAULT_ALLOW_OVERRIDE,
                DEFAULT_INJECT_PROXY_BASE,
                DEFAULT_REMOTE_RESOURCES,
                DEFAULT_FRAME_ANCESTORS,
                new ArrayList<>());
    }

    /**
     * Creates a new CSPConfiguration object with the provided values.
     *
     * @param enabled whether the Content-Security-Policy header is enabled
     * @param reportOnly whether to report CSP violations without enforcing the policy
     * @param allowOverride whether to allow other components to completely override the CSP
     * @param injectProxyBase whether to inject the proxy base URL into fetch directives
     * @param remoteResources remote hosts to add to fetch directives for static web files
     * @param frameAncestors hosts to add to the frame-ancestors directive
     * @param policies the policies for individual Content-Security-Policy headers
     */
    public CSPConfiguration(
            boolean enabled,
            boolean reportOnly,
            boolean allowOverride,
            boolean injectProxyBase,
            String remoteResources,
            String frameAncestors,
            List<CSPPolicy> policies) {
        this.enabled = enabled;
        this.reportOnly = reportOnly;
        this.allowOverride = allowOverride;
        this.injectProxyBase = injectProxyBase;
        this.remoteResources = remoteResources;
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
                other.isReportOnly(),
                other.isAllowOverride(),
                other.isInjectProxyBase(),
                other.getRemoteResources(),
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

    /** @return whether to report CSP violations without enforcing the policy */
    public boolean isReportOnly() {
        return this.reportOnly;
    }

    /** @param reportOnly whether to report CSP violations without enforcing the policy */
    public void setReportOnly(boolean reportOnly) {
        this.reportOnly = reportOnly;
    }

    /** @return whether to allow other components to completely override the CSP */
    public boolean isAllowOverride() {
        return this.allowOverride;
    }

    /** @param allowOverride whether to allow other components to completely override the CSP */
    public void setAllowOverride(boolean allowOverride) {
        this.allowOverride = allowOverride;
    }

    /** @return whether to inject the proxy base URL into fetch directives */
    public boolean isInjectProxyBase() {
        return this.injectProxyBase;
    }

    /** @param injectProxyBase whether to inject the proxy base URL into fetch directives */
    public void setInjectProxyBase(boolean injectProxyBase) {
        this.injectProxyBase = injectProxyBase;
    }

    /** @return remote hosts to add to fetch directives for static web files */
    public String getRemoteResources() {
        return this.remoteResources;
    }

    /** @param externalResources remote hosts to add to fetch directives for static web files */
    public void setRemoteResources(String externalResources) {
        this.remoteResources = externalResources;
    }

    /** @return hosts to add to the frame-ancestors directive */
    public String getFrameAncestors() {
        return this.frameAncestors;
    }

    /** @param frameAncestors hosts to add to the frame-ancestors directive */
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
     * Gets the value of the field that matches the specific key, trimming leading and trailing whitespace from the
     * value and returns that value or an empty string if no field matches the key.
     *
     * @param key the field key
     * @return the field value or an empty string
     */
    public String getField(String key) {
        switch (key) {
            case CSPUtils.GEOSERVER_CSP_REMOTE_RESOURCES:
                return CSPUtils.trimWhitespace(this.remoteResources);
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
        getPolicies().stream().map(CSPPolicy::getRules).flatMap(List::stream).forEach(CSPRule::parseFilter);
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CSPConfiguration) {
            CSPConfiguration other = (CSPConfiguration) obj;
            return Objects.equals(this.enabled, other.enabled)
                    && Objects.equals(this.reportOnly, other.reportOnly)
                    && Objects.equals(this.allowOverride, other.allowOverride)
                    && Objects.equals(this.injectProxyBase, other.injectProxyBase)
                    && Objects.equals(this.remoteResources, other.remoteResources)
                    && Objects.equals(this.frameAncestors, other.frameAncestors)
                    && Objects.equals(this.policies, other.policies);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.enabled,
                this.reportOnly,
                this.allowOverride,
                this.injectProxyBase,
                this.remoteResources,
                this.frameAncestors,
                this.policies);
    }

    /** Initialize after XStream deserialization */
    private Object readResolve() {
        this.enabled = firstNonNull(this.enabled, DEFAULT_ENABLED);
        this.reportOnly = firstNonNull(this.reportOnly, DEFAULT_REPORT_ONLY);
        this.allowOverride = firstNonNull(this.allowOverride, DEFAULT_ALLOW_OVERRIDE);
        this.injectProxyBase = firstNonNull(this.injectProxyBase, DEFAULT_INJECT_PROXY_BASE);
        this.remoteResources = firstNonNull(this.remoteResources, DEFAULT_REMOTE_RESOURCES);
        this.frameAncestors = firstNonNull(this.frameAncestors, DEFAULT_FRAME_ANCESTORS);
        this.policies = firstNonNull(this.policies, new ArrayList<>());
        long count = this.policies.stream().map(CSPPolicy::getName).distinct().count();
        Preconditions.checkArgument(count == this.policies.size(), "Policy names must be unique");
        return parseFilters();
    }
}
