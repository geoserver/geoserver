/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/** A class that holds a list of {@link SchemaRule}. Is meant to be stored in the FeatureTypeInfo metadata map. */
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
