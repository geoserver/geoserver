/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A class that holds a list of {@link TemplateRule}. Is meant to be stored in the FeatureTypeInfo
 * metadata map.
 */
@XmlRootElement(name = "TemplateLayerConfig")
public class TemplateLayerConfig implements Serializable {

    public static final String METADATA_KEY = "FEATURES_TEMPLATING_LAYER_CONF";

    @XmlElement(name = "Rule")
    private Set<TemplateRule> templateRules;

    public TemplateLayerConfig(Set<TemplateRule> templateRules) {
        this.templateRules = templateRules;
    }

    public TemplateLayerConfig() {
        templateRules = new HashSet<>();
    }

    public void addTemplateRule(TemplateRule rule) {
        if (this.templateRules == null) templateRules = new HashSet<>();
        this.templateRules.add(rule);
    }

    public Set<TemplateRule> getTemplateRules() {
        if (this.templateRules == null) this.templateRules = new HashSet<>();
        return templateRules;
    }

    public void setTemplateRules(Set<TemplateRule> templateRules) {
        this.templateRules = templateRules;
    }
}
