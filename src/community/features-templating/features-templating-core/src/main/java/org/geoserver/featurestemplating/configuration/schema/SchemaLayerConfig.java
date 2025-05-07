/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.geoserver.featurestemplating.configuration.TemplateRule;

/** A class that holds a list of {@link TemplateRule}. Is meant to be stored in the FeatureTypeInfo metadata map. */
@XmlRootElement(name = "SchemaLayerConfig")
public class SchemaLayerConfig implements Serializable {

    public static final String METADATA_KEY = "FEATURES_SCHEMA_LAYER_CONF";

    @XmlElement(name = "Rule")
    private Set<SchemaRule> schemaRules;

    public SchemaLayerConfig(Set<SchemaRule> schemaRules) {
        this.schemaRules = schemaRules;
    }

    public SchemaLayerConfig() {
        schemaRules = new HashSet<>();
    }

    public void addSchemaRule(SchemaRule rule) {
        if (this.schemaRules == null) schemaRules = new HashSet<>();
        this.schemaRules.add(rule);
    }

    public Set<SchemaRule> getSchemaRules() {
        if (this.schemaRules == null) this.schemaRules = new HashSet<>();
        return schemaRules;
    }

    public void setSchemaRules(Set<SchemaRule> schemaRules) {
        this.schemaRules = schemaRules;
    }
}
