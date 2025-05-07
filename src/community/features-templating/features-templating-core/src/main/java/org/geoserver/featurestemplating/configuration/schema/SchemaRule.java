/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;
import javax.xml.bind.annotation.XmlRootElement;
import org.geoserver.featurestemplating.configuration.SupportedFormat;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.ows.Request;
import org.geoserver.util.XCQL;
import org.geotools.api.filter.Filter;
import org.geotools.filter.text.cql2.CQLException;

/**
 * A template rule associated to a FeatureTypeInfo. Its evaluation determines if a specific template should be applied
 * for a Request.
 */
@XmlRootElement(name = "Rule")
public class SchemaRule implements Serializable {

    private String ruleId;

    private Integer priority;

    private String schemaIdentifier;

    private String schemaName;

    private String outputFormat;

    private String service;

    private String cqlFilter;

    private String profileFilter;

    // use to force a rule to be applied regardless of priority
    // currently used only from the preview mechanism in the web module.
    private boolean forceRule;

    public SchemaRule() {
        this.priority = 0;
        this.ruleId = UUID.randomUUID().toString();
    }

    public SchemaRule(SchemaRule rule) {
        this.ruleId = rule.ruleId == null ? UUID.randomUUID().toString() : rule.ruleId;
        this.priority = rule.priority;
        this.outputFormat = rule.outputFormat;
        this.cqlFilter = rule.cqlFilter;
        this.service = rule.service;
        this.forceRule = rule.forceRule;
        this.schemaName = rule.schemaName;
        this.schemaIdentifier = rule.schemaIdentifier;
        this.profileFilter = rule.profileFilter;
    }

    public String getSchemaName() {
        return schemaName;
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

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
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

    public String getSchemaIdentifier() {
        return schemaIdentifier;
    }

    public void setSchemaIdentifier(String schemaIdentifier) {
        this.schemaIdentifier = schemaIdentifier;
    }

    private boolean matchOutputFormat(String outputFormat) {
        TemplateIdentifier identifier = TemplateIdentifier.fromOutputFormat(outputFormat);
        if (identifier == null) return false;
        String nameIdentifier = identifier.name();
        if (this.outputFormat.equals(SupportedFormat.GML.name())) return nameIdentifier.startsWith(this.outputFormat);
        else if (this.outputFormat.equals(SupportedFormat.GEOJSON.name()))
            return nameIdentifier.equals(TemplateIdentifier.GEOJSON.name())
                    || nameIdentifier.equals(TemplateIdentifier.JSON.name());
        else if (this.outputFormat.equals(SupportedFormat.HTML.name()))
            return nameIdentifier.equals(TemplateIdentifier.HTML.name());
        else return nameIdentifier.equals(this.outputFormat);
    }

    public void setSchemaInfo(SchemaInfo schemaInfo) {
        if (schemaInfo != null) {
            this.schemaName = schemaInfo.getFullName();
            this.schemaIdentifier = schemaInfo.getIdentifier();
        }
    }

    /**
     * Return the TemplateInfo to which this rule refers to.
     *
     * @return the TemplateInfo associated to the rule.
     */
    public SchemaInfo getSchemaInfo() {
        SchemaInfo ti = new SchemaInfo();
        if (schemaName != null && schemaName.indexOf(":") != -1) {
            String[] nameSplit = schemaName.split(":");
            if (nameSplit.length == 3) {
                ti.setWorkspace(nameSplit[0]);
                ti.setFeatureType(nameSplit[1]);
                ti.setSchemaName(nameSplit[2]);
            } else {
                ti.setWorkspace(nameSplit[0]);
                ti.setSchemaName(nameSplit[1]);
            }
        }
        ti.setIdentifier(schemaIdentifier);
        return ti;
    }

    private String getOutputFormat(Request request) {
        String outputFormat = request.getOutputFormat();
        if (outputFormat == null)
            outputFormat = request.getKvp() != null ? (String) request.getKvp().get("f") : null;
        if (outputFormat == null)
            outputFormat = request.getKvp() != null ? (String) request.getKvp().get("INFO_FORMAT") : null;
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
        SchemaRule that = (SchemaRule) o;
        return Objects.equals(schemaIdentifier, that.schemaIdentifier)
                && Objects.equals(schemaName, that.schemaName)
                && Objects.equals(outputFormat, that.outputFormat)
                && Objects.equals(service, that.service)
                && Objects.equals(profileFilter, that.profileFilter)
                && Objects.equals(cqlFilter, that.cqlFilter)
                && Objects.equals(priority, that.priority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemaIdentifier, schemaName, outputFormat, service, priority);
    }

    /**
     * Rule comparator to sort the TemplateRules in order to get the one with higher priority or the one that is forced.
     */
    public static class SchemaRuleComparator implements Comparator<SchemaRule> {

        @Override
        public int compare(SchemaRule o1, SchemaRule o2) {
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
