/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.configuration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.eclipse.emf.common.util.URI;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.jsonld.builders.impl.RootBuilder;
import org.geoserver.jsonld.validation.JsonLdValidator;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.complex.feature.type.ComplexFeatureTypeImpl;
import org.geotools.feature.type.Types;
import org.opengis.feature.type.FeatureType;
import org.xml.sax.helpers.NamespaceSupport;

/** Manage the cache and the retrieving of all json-ld template files related */
public class JsonLdConfiguration {

    private final LoadingCache<CacheKey, JsonLdTemplate> templateCache;
    private GeoServerDataDirectory dataDirectory;
    public static final String JSON_LD_NAME = "json-ld-template.json";

    public JsonLdConfiguration(GeoServerDataDirectory dd) {
        this.dataDirectory = dd;
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
                                            namespaces = declareNamespaces(type);
                                        } catch (IOException e) {
                                            throw new RuntimeException(
                                                    "Error retrieving FeatureType "
                                                            + key.getResource().getName()
                                                            + "Exception is: "
                                                            + e.getMessage());
                                        }
                                        Resource resource =
                                                getDataDirectory()
                                                        .get(key.getResource(), key.getPath());
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
                                + URI.decode(validator.getFailingAttribute()));
            }
        } else {
            throw new RuntimeException(
                    "No Json-Ld template found for feature type " + typeInfo.getName());
        }
        return root;
    }

    /**
     * Extract Namespaces from given FeatureType
     *
     * @return Namespaces if found for the given FeatureType
     */
    private NamespaceSupport declareNamespaces(FeatureType type) {
        NamespaceSupport namespaceSupport = null;
        if (type instanceof ComplexFeatureTypeImpl) {
            Map namespaces = (Map) type.getUserData().get(Types.DECLARED_NAMESPACES_MAP);
            if (namespaces != null) {
                namespaceSupport = new NamespaceSupport();
                for (Iterator it = namespaces.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    String prefix = (String) entry.getKey();
                    String namespace = (String) entry.getValue();
                    namespaceSupport.declarePrefix(prefix, namespace);
                }
            }
        }
        return namespaceSupport;
    }

    GeoServerDataDirectory getDataDirectory() {
        return dataDirectory;
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
