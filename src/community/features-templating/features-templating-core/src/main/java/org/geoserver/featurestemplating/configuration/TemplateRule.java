/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import javax.xml.bind.annotation.XmlRootElement;
import org.geoserver.ows.Request;
import org.geoserver.util.XCQL;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.cql2.CQLException;

/**
 * A template rule associated to a FeatureTypeInfo. Its evaluation determines if a specific template
 * should be applied for a Request.
 */
@XmlRootElement(name = "Rule")
public class TemplateRule implements Serializable {

    private String ruleId;

    private Integer priority;

    private String templateIdentifier;

    private String templateName;

    private String outputFormat;

    private String service;

    private String cqlFilter;

    private String profileFilter;

    // use to force a rule to be applied regardless of priority
    // currently used only from the preview mechanism in the web module.
    private boolean forceRule;

    public TemplateRule() {
        this.priority = 0;
        this.ruleId = UUID.randomUUID().toString();
    }

    public TemplateRule(TemplateRule rule) {
        this.ruleId = rule.ruleId == null ? UUID.randomUUID().toString() : rule.ruleId;
        this.priority = rule.priority;
        this.outputFormat = rule.outputFormat;
        this.cqlFilter = rule.cqlFilter;
        this.service = rule.service;
        this.forceRule = rule.forceRule;
        this.templateName = rule.templateName;
        this.templateIdentifier = rule.templateIdentifier;
        this.profileFilter = rule.profileFilter;
    }

    public String getTemplateName() {
        return templateName;
    }

    /**
     * Apply the rule to the Request to see if it matches it.
     *
     * @param request the request against which evaluate the rule.
     * @return
     */
    public boolean applyRule(Request request) {
        boolean result = true;
        if (outputFormat != null) result = matchOutputFormat(getOutputFormat(request));

        if (result && cqlFilter != null) {
            result = evaluateCQLFilter(cqlFilter, request);
        }
        if (result && profileFilter != null) result = evaluateCQLFilter(profileFilter, request);

        return result;
    }

    private boolean evaluateCQLFilter(String filter, Request request) {
        return getCQLFilter(filter).evaluate(request);
    }

    private Filter getCQLFilter(String filter) {
        try {
            return XCQL.toFilter(filter);
        } catch (CQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public SupportedFormat getOutputFormat() {
        if (outputFormat != null) return SupportedFormat.valueOf(outputFormat);
        return null;
    }

    public void setOutputFormat(SupportedFormat outputFormat) {
        this.outputFormat = outputFormat.name();
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getCqlFilter() {
        return cqlFilter;
    }

    public void setCqlFilter(String cqlFilter) {
        this.cqlFilter = cqlFilter;
    }

    public String getTemplateIdentifier() {
        return templateIdentifier;
    }

    public void setTemplateIdentifier(String templateIdentifier) {
        this.templateIdentifier = templateIdentifier;
    }

    private boolean matchOutputFormat(String outputFormat) {
        TemplateIdentifier identifier = TemplateIdentifier.fromOutputFormat(outputFormat);
        if (identifier == null) return false;
        String nameIdentifier = identifier.name();
        if (this.outputFormat.equals(SupportedFormat.GML.name()))
            return nameIdentifier.startsWith(this.outputFormat);
        else if (this.outputFormat.equals(SupportedFormat.GEOJSON.name()))
            return nameIdentifier.equals(TemplateIdentifier.GEOJSON.name())
                    || nameIdentifier.equals(TemplateIdentifier.JSON.name());
        else if (this.outputFormat.equals(SupportedFormat.HTML.name()))
            return nameIdentifier.equals(TemplateIdentifier.HTML.name());
        else return nameIdentifier.equals(this.outputFormat);
    }

    public void setTemplateInfo(TemplateInfo templateInfo) {
        if (templateInfo != null) {
            this.templateName = templateInfo.getFullName();
            this.templateIdentifier = templateInfo.getIdentifier();
        }
    }

    /**
     * Return the TemplateInfo to which this rule refers to.
     *
     * @return the TemplateInfo associated to the rule.
     */
    public TemplateInfo getTemplateInfo() {
        TemplateInfo ti = new TemplateInfo();
        if (templateName != null && templateName.indexOf(":") != -1) {
            String[] nameSplit = templateName.split(":");
            if (nameSplit.length == 3) {
                ti.setWorkspace(nameSplit[0]);
                ti.setFeatureType(nameSplit[1]);
                ti.setTemplateName(nameSplit[2]);
            } else {
                ti.setWorkspace(nameSplit[0]);
                ti.setTemplateName(nameSplit[1]);
            }
        }
        ti.setIdentifier(templateIdentifier);
        return ti;
    }

    private String getOutputFormat(Request request) {
        String outputFormat = request.getOutputFormat();
        if (outputFormat == null)
            outputFormat = request.getKvp() != null ? (String) request.getKvp().get("f") : null;
        if (outputFormat == null)
            outputFormat =
                    request.getKvp() != null ? (String) request.getKvp().get("INFO_FORMAT") : null;
        return outputFormat;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public boolean isForceRule() {
        return forceRule;
    }

    public void setForceRule(boolean forceRule) {
        this.forceRule = forceRule;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getProfileFilter() {
        return profileFilter;
    }

    public void setProfileFilter(String profileFilter) {
        this.profileFilter = profileFilter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateRule that = (TemplateRule) o;
        return Objects.equals(templateIdentifier, that.templateIdentifier)
                && Objects.equals(templateName, that.templateName)
                && Objects.equals(outputFormat, that.outputFormat)
                && Objects.equals(service, that.service)
                && Objects.equals(profileFilter, that.profileFilter)
                && Objects.equals(cqlFilter, that.cqlFilter)
                && Objects.equals(priority, that.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                templateIdentifier, templateName, outputFormat, service, cqlFilter, priority);
    }

    /**
     * Rule comparator to sort the TemplateRules in order to get the one with higher priority or the
     * one that is forced.
     */
    public static class TemplateRuleComparator implements Comparator<TemplateRule> {

        @Override
        public int compare(TemplateRule o1, TemplateRule o2) {
            int result;
            if (o1.isForceRule()) result = -1;
            else if (o2.isForceRule()) result = 1;
            else {
                int p1 = o1.getPriority();
                int p2 = o2.getPriority();
                if (p1 < p2) result = -1;
                else if (p2 < p1) result = 1;
                else result = 0;
            }
            return result;
        }
    }
}
