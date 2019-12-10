/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.configuration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.validation.JsonLdValidator;
import org.geoserver.platform.resource.Resource;

/** Manage the cache and the retrieving of all json-ld template files related */
public class JsonLdConfiguration {

    private final LoadingCache<CacheKey, JsonLdTemplate> templateCache;
    private GeoServerDataDirectory dd;

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
                                        Resource resource =
                                                dd.get(key.getResource(), key.getPath());
                                        JsonLdTemplate template = new JsonLdTemplate(resource);
                                        return template;
                                    }
                                });
    }

    /**
     * Get the template related to the featureType. If template has benn modified updates the cache
     * with the new JsonLdTemplate
     *
     * @param resource
     * @param path
     * @return
     * @throws ExecutionException
     */
    public RootBuilder getTemplate(FeatureTypeInfo resource, String path)
            throws ExecutionException {
        CacheKey key = new CacheKey(resource, path);
        JsonLdTemplate template = templateCache.get(key);
        if (template.checkTemplate(resource)) templateCache.put(key, template);
        boolean isValid;
        RootBuilder root = template.getRootBuilder();
        if (root != null) {
            JsonLdValidator validator = new JsonLdValidator(resource);
            isValid = validator.validateTemplate(root);
            if (!isValid) {
                throw new RuntimeException(
                        "Failed to validate json-ld template for feature type "
                                + resource.getName()
                                + ". Failing attribute is "
                                + validator.getFailingAttribute());
            }
        } else {
            throw new RuntimeException(
                    "No Json-Ld template found for feature type " + resource.getName());
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

        public void setResource(FeatureTypeInfo resource) {
            this.resource = resource;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
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
