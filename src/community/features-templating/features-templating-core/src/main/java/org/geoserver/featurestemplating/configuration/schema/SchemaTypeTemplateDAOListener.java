/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.util.Optional;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.GeoServerExtensions;

/** A Template event listener that handle Template rules update when a TemplateInfo is deleted or modified. */
public class SchemaTypeTemplateDAOListener implements SchemaDAOListener {

    private FeatureTypeInfo fti;

    public SchemaTypeTemplateDAOListener(FeatureTypeInfo featureTypeInfo) {
        this.fti = featureTypeInfo;
    }

    @Override
    public void handleDeleteEvent(SchemaInfoEvent deleteEvent) {
        SchemaLayerConfig layerConfig = fti.getMetadata().get(SchemaLayerConfig.METADATA_KEY, SchemaLayerConfig.class);
        SchemaInfo ti = deleteEvent.getSource();
        if (layerConfig != null) {
            Set<SchemaRule> rules = layerConfig.getSchemaRules();
            if (!rules.isEmpty()) {
                if (rules.removeIf(r ->
                        r.getSchemaIdentifier().equals(deleteEvent.getSource().getIdentifier()))) {
                    fti.getMetadata().put(SchemaLayerConfig.METADATA_KEY, layerConfig);
                    saveFeatureTypeInfo();
                    updateCache(ti);
                }
            }
        }
    }

    @Override
    public void handleUpdateEvent(SchemaInfoEvent updateEvent) {
        SchemaLayerConfig layerConfig = fti.getMetadata().get(SchemaLayerConfig.METADATA_KEY, SchemaLayerConfig.class);
        if (layerConfig != null) {
            Set<SchemaRule> rules = layerConfig.getSchemaRules();
            if (!rules.isEmpty()) {
                SchemaInfo info = updateEvent.getSource();
                Optional<SchemaRule> rule = rules.stream()
                        .filter(r -> r.getSchemaIdentifier().equals(info.getIdentifier()))
                        .findFirst();
                if (rule.isPresent()) {
                    SchemaRule r = rule.get();
                    if (!r.getSchemaName().equals(info.getFullName())) r.setSchemaName(info.getFullName());
                    updateCache(info);
                    rules.removeIf(tr -> tr.getSchemaIdentifier().equals(info.getIdentifier()));
                    rules.add(r);
                    layerConfig.setSchemaRules(rules);
                    fti.getMetadata().put(SchemaLayerConfig.METADATA_KEY, layerConfig);
                    saveFeatureTypeInfo();
                }
            }
        }
    }

    private void saveFeatureTypeInfo() {
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        catalog.save(fti);
    }

    private void updateCache(SchemaInfo info) {
        SchemaLoader loader = SchemaLoader.get();
        loader.cleanCache(fti, info.getIdentifier());
    }
}
