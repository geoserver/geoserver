/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.GeoServerExtensions;

/** Class that provides methods to add, update or delete Template Rules */
public class TemplateRuleService {

    private FeatureTypeInfo featureTypeInfo;

    public TemplateRuleService(FeatureTypeInfo featureTypeInfo) {
        this.featureTypeInfo = featureTypeInfo;
    }

    /**
     * Remove the template rule having the specified id.
     *
     * @param ruleId the id of the rule to delete.
     * @return true if the rule was deleted false if the rule was not found.
     */
    public boolean removeRule(String ruleId) {
        boolean result = false;
        Set<TemplateRule> rules = getRules();
        if (rules != null && !rules.isEmpty()) {
            result = rules.removeIf(r -> r.getRuleId().equals(ruleId));
            if (result) {
                TemplateLayerConfig config = getTemplateLayerConfig();
                config.setTemplateRules(rules);
                featureTypeInfo.getMetadata().put(TemplateLayerConfig.METADATA_KEY, config);
                getCatalog().save(featureTypeInfo);
            }
        }
        return result;
    }

    /**
     * Replace the rule with the one passed as an argument if they have the same id.
     *
     * @param rule the rule to use as a replacement for the one with same id.
     */
    public void replaceRule(TemplateRule rule) {
        Set<TemplateRule> rules = getRules();
        if (rules != null) {
            if (rules.removeIf((r -> r.getRuleId().equals(rule.getRuleId())))) {
                Set<TemplateRule> ruleset = updatePriorities(new ArrayList<>(rules), rule);
                TemplateLayerConfig config = getTemplateLayerConfig();
                config.setTemplateRules(ruleset);
                featureTypeInfo.getMetadata().put(TemplateLayerConfig.METADATA_KEY, config);
                getCatalog().save(featureTypeInfo);
            }
        }
    }

    /**
     * Save a rule.
     *
     * @param rule the rule to save.
     */
    public void saveRule(TemplateRule rule) {
        TemplateLayerConfig config = getTemplateLayerConfig();
        if (config == null) config = new TemplateLayerConfig();
        Set<TemplateRule> rules = config.getTemplateRules();
        Set<TemplateRule> ruleset = updatePriorities(new ArrayList<>(rules), rule);
        config.setTemplateRules(ruleset);
        featureTypeInfo.getMetadata().put(TemplateLayerConfig.METADATA_KEY, config);
        getCatalog().save(featureTypeInfo);
    }

    private TemplateLayerConfig getTemplateLayerConfig() {
        return featureTypeInfo
                .getMetadata()
                .get(TemplateLayerConfig.METADATA_KEY, TemplateLayerConfig.class);
    }

    public Set<TemplateRule> getRules() {
        TemplateLayerConfig layerConfig = getTemplateLayerConfig();
        if (layerConfig != null) return layerConfig.getTemplateRules();
        return Collections.emptySet();
    }

    /**
     * Get the template rule having the specified id.
     *
     * @param ruleId the id of the rule to find.
     * @return the rule or null if not found.
     */
    public TemplateRule getRule(String ruleId) {
        Set<TemplateRule> rules = getRules();
        if (rules != null && !rules.isEmpty()) {
            Optional<TemplateRule> opRule =
                    rules.stream().filter(r -> r.getRuleId().equals(ruleId)).findFirst();
            if (opRule.isPresent()) return opRule.get();
        }
        return null;
    }

    private Catalog getCatalog() {
        return (Catalog) GeoServerExtensions.bean("catalog");
    }

    /**
     * Update the priorities of the existing rules based on the priority of the new Rule. It shift
     * by one position the priority value of the rules having it >= the priority value of the new
     * Rule.
     *
     * @param rules the already existing rule.
     * @param newRule the new Rule to be added.
     * @return a Set of rules having the priority fields updated.
     */
    public static Set<TemplateRule> updatePriorities(
            List<TemplateRule> rules, TemplateRule newRule) {
        Set<TemplateRule> set = new HashSet<>(rules.size());
        int updatedPriority = newRule.getPriority();
        boolean newRuleAdded = false;
        for (TemplateRule rule : rules) {
            boolean isUpdating = rule.getRuleId().equals(newRule.getRuleId());
            int priority = rule.getPriority();
            if (priority == updatedPriority) {
                if (!newRuleAdded) {
                    set.add(newRule);
                    newRuleAdded = true;
                }
                priority++;
                if (!isUpdating) {
                    rule.setPriority(priority);
                    updatedPriority = priority;
                }
            }
            if (!isUpdating) set.add(rule);
        }
        if (set.isEmpty() || !newRuleAdded) set.add(newRule);
        return set;
    }
}
