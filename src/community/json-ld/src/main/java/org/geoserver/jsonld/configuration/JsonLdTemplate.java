/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.configuration;

import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.jsonld.builders.JsonBuilder;
import org.geoserver.jsonld.builders.impl.DynamicValueBuilder;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.builders.impl.StaticBuilder;
import org.geoserver.platform.resource.Resource;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.expression.PropertyName;

public class JsonLdTemplate {

    private Resource templateFile;
    private JsonLdTemplateWatcher watcher;
    private RootBuilder builderTree;

    private static final Logger LOGGER = Logging.getLogger(JsonLdTemplate.class);

    public JsonLdTemplate(Resource templateFile) {
        this.templateFile = templateFile;
        this.watcher = new JsonLdTemplateWatcher(templateFile);
        try {
            this.builderTree = watcher.getJsonLdTemplateParser();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }


    /**
     * Check if json-ld template file has benn modified an eventually reload it
     *
     * @param typeInfo
     * @return
     */
    public boolean checkTemplate(FeatureTypeInfo typeInfo) {
        if (watcher != null && watcher.isModified()) {
            LOGGER.log(
                    Level.INFO,
                    "Reloading json-ld template for Feature Type {0}",
                    templateFile.name());
            synchronized (this) {
                if (watcher != null && watcher.isModified()) {
                    try {
                        RootBuilder root = watcher.getJsonLdTemplateParser();
                        boolean isValid = validateTemplate(typeInfo.getFeatureType(), root);
                        if (isValid) {
                            this.builderTree = root;
                            return true;
                        } else
                            LOGGER.log(
                                    Level.INFO,
                                    "Json-ld template for feature type "
                                            + typeInfo.getName()
                                            + " didn't pass validation. Skipping loading");
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }
            }
        }
        return false;
    }

    public RootBuilder getBuilderTree() {
        return builderTree;
    }

    private boolean validateTemplate(FeatureType type, RootBuilder root) {

        FilterAttributeExtractor visitor = null;
        if (type instanceof SimpleFeatureType) {
            visitor = new FilterAttributeExtractor((SimpleFeatureType) type);
        } else {
            visitor = new FilterAttributeExtractor();
        }
        validateExpressions(root, visitor);
        Set<String> attributes = visitor.getAttributeNameSet();
        return true;
    }

    private void validateExpressions(JsonBuilder builder, FilterAttributeExtractor visitor) {
        for (JsonBuilder jb : builder.getChildren()) {
            if (jb instanceof DynamicValueBuilder) {
                DynamicValueBuilder djb = (DynamicValueBuilder) jb;
                if (djb.getExpression() != null) {
                    try {
                        djb.getExpression().accept(visitor, null);
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.INFO,
                                "CQL validation failed for function {0}",
                                djb.getExpression());
                    }
                } else if (djb.getXpath() != null) {
                    PropertyName propertyName =
                            new AttributeExpressionImpl(djb.getXpath(), RootBuilder.namespaces);
                    try {
                        propertyName.accept(visitor, null);
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.INFO,
                                "Xpath validation failed for {0}",
                                propertyName.getPropertyName());
                    }
                }
            } else {
                if (!(jb instanceof StaticBuilder || jb instanceof DynamicValueBuilder)) {
                    validateExpressions(jb, visitor);
                }
            }
        }
    }
}
