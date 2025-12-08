/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;

public class SchemaXStreamConfigurationMigration implements BeanPostProcessor, Ordered {

    private static final Logger LOGGER = Logging.getLogger(SchemaXStreamConfigurationMigration.class);

    private static final String FEATURETYPE_FILE = "featuretype.xml";

    // List of element tag names that require migration (without angle brackets)
    private static final String[] ELEMENT_TAGS = new String[] {
        "Rule",
        "TemplateLayerConfig",
        "TemplateRuleType",
        "LayerConfigType",
        "SchemaRule",
        "SchemaLayerConfig",
        "SchemaRuleType",
        "SchemaLayerConfigType"
    };

    // Programmatically built replacements from ELEMENT_TAGS -> opening and closing tag replacements
    private static final String[][] REPLACEMENTS = buildReplacements();

    private final GeoServerResourceLoader resourceLoader;

    public SchemaXStreamConfigurationMigration(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "Resource loader cannot be null");
    }

    private static String[][] buildReplacements() {
        List<String[]> reps = new ArrayList<>();
        for (String tag : ELEMENT_TAGS) {
            reps.add(new String[] {"<" + tag + ">", "<Ft" + tag + ">"});
            reps.add(new String[] {"</" + tag + ">", "</Ft" + tag + ">"});
        }
        return reps.toArray(new String[0][]);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (beanName.equals("configurationLock")) {
            try {
                migrateFeatureTypeConfigurations();
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Unexpected error while migrating features templating schema elements during startup. "
                                + "GeoServer will continue startup, but templates might not be migrated.",
                        e);
            }
        }
        return bean;
    }

    private void migrateFeatureTypeConfigurations() {
        LOGGER.info(() -> "Starting features templating schema migration using data directory at "
                + resourceLoader.getBaseDirectory());
        Resource workspaces = resourceLoader.get("workspaces");
        if (workspaces == null || workspaces.getType() != Resource.Type.DIRECTORY) {
            LOGGER.fine(
                    () ->
                            "No workspaces directory found in the data directory, skipping features templating schema migration.");
            return;
        }
        try {
            scanWorkspaceResources(workspaces);
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Failed while scanning data directory for features templating schema migration at "
                            + workspaces.path(),
                    e);
        }
    }

    private void scanWorkspaceResources(Resource resource) {
        if (resource.getType() == Resource.Type.DIRECTORY) {
            LOGGER.fine(() -> "Scanning directory " + resource.path() + " for feature type configurations.");
            for (Resource child : resource.list()) {
                scanWorkspaceResources(child);
            }
            return;
        }
        if (FEATURETYPE_FILE.equals(resource.name())) {
            migrateFeatureTypeResource(resource);
        }
    }

    private void migrateFeatureTypeResource(Resource featureTypeResource) {
        LOGGER.fine(() -> "Checking feature type configuration at " + featureTypeResource.path());
        try {
            String originalContent = new String(featureTypeResource.getContents(), StandardCharsets.UTF_8);
            String migratedContent = applySchemaReplacements(originalContent);
            if (!migratedContent.equals(originalContent)) {
                Resource.Lock lock = featureTypeResource.lock();
                try {
                    featureTypeResource.setContents(migratedContent.getBytes(StandardCharsets.UTF_8));
                    LOGGER.info(() -> "Updated templating schema elements in " + featureTypeResource.path());
                } finally {
                    lock.release();
                }
            } else {
                LOGGER.finer(() -> "No templating schema changes required for " + featureTypeResource.path());
            }
        } catch (IOException e) {
            LOGGER.log(
                    Level.WARNING, "Failed to migrate templating schema elements in " + featureTypeResource.path(), e);
        }
    }

    private String applySchemaReplacements(String content) {
        String updated = content;
        for (String[] replacement : REPLACEMENTS) {
            updated = updated.replace(replacement[0], replacement[1]);
        }
        return updated;
    }
}
