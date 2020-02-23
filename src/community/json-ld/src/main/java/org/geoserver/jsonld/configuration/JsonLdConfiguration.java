/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.configuration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.expressions.ExpressionsUtils;
import org.geoserver.jsonld.validation.JsonLdValidator;
import org.geoserver.platform.resource.Resource;
import org.opengis.feature.type.FeatureType;
import org.xml.sax.helpers.NamespaceSupport;

/** Manage the cache and the retrieving of all json-ld template files related */
public class JsonLdConfiguration {

    private final LoadingCache<CacheKey, JsonLdTemplate> templateCache;
    private GeoServerDataDirectory dd;
    public static final String JSON_LD_NAME = "json-ld-template.json";

    public JsonLdConfiguration(GeoServerDataDirectory dd) {
        this.dd = dd;
        templateCache =
                CacheBuilder.newBuilder()
                        .maximumSize(100)
                        .initialCapacity(1)
                        .expireAfterAccess(120, TimeUnit.MINUTES)
                        .build(
                                new CacheLoader<CacheKey, JsonLdTemplate>() {
                                    @Override
                                    public JsonLdTemplate load(CacheKey key) {
                                        NamespaceSupport namespaces = null;
                                        try {
                                            FeatureType type = key.getResource().getFeatureType();
                                            namespaces = ExpressionsUtils.declareNamespaces(type);
                                        } catch (IOException e) {
                                            throw new RuntimeException(
                                                    "Error retrieving FeatureType "
                                                            + key.getResource().getName()
                                                            + "Exception is: "
                                                            + e.getMessage());
                                        }
                                        Resource resource =
                                                dd.get(key.getResource(), key.getPath());
                                        JsonLdTemplate template =
                                                new JsonLdTemplate(resource, namespaces);
                                        return template;
                                    }
                                });
    }

    /**
     * Get the template related to the featureType. If template has benn modified updates the cache
     * with the new JsonLdTemplate
     */
    public RootBuilder getTemplate(FeatureTypeInfo typeInfo) throws ExecutionException {
        CacheKey key = new CacheKey(typeInfo, JSON_LD_NAME);
        JsonLdTemplate template = templateCache.get(key);
        if (template.checkTemplate()) templateCache.put(key, template);
        boolean isValid;
        RootBuilder root = template.getRootBuilder();
        if (root != null) {
            JsonLdValidator validator = new JsonLdValidator(typeInfo);
            isValid = validator.validateTemplate(root);
            if (!isValid) {
                throw new RuntimeException(
                        "Failed to validate json-ld template for feature type "
                                + typeInfo.getName()
                                + ". Failing attribute is "
                                + validator.getFailingAttribute());
            }
        } else {
            throw new RuntimeException(
                    "No Json-Ld template found for feature type " + typeInfo.getName());
        }
        return root;
    }

    private class CacheKey {
        private FeatureTypeInfo resource;
        private String path;

        public CacheKey(FeatureTypeInfo reosurce, String path) {
            this.resource = reosurce;
            this.path = path;
        }

        public FeatureTypeInfo getResource() {
            return resource;
        }

        public String getPath() {
            return path;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CacheKey)) return false;
            CacheKey other = (CacheKey) o;
            if (!other.getPath().equals(path)) return false;
            else if (!(other.getResource().getName().equals(resource.getName()))) return false;
            else if (!(other.getResource().getNamespace().equals(resource.getNamespace())))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(resource, path);
        }
    }
}
