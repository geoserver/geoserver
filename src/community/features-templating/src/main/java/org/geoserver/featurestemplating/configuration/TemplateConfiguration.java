/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration;

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
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.validation.TemplateValidator;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.complex.feature.type.ComplexFeatureTypeImpl;
import org.geotools.data.complex.feature.type.Types;
import org.opengis.feature.type.FeatureType;
import org.xml.sax.helpers.NamespaceSupport;

/** Manage the cache and the retrieving for all templates files */
public class TemplateConfiguration {

    private final LoadingCache<CacheKey, WFSTemplate> templateCache;
    private GeoServerDataDirectory dataDirectory;

    public TemplateConfiguration(GeoServerDataDirectory dd) {
        this.dataDirectory = dd;
        templateCache =
                CacheBuilder.newBuilder()
                        .maximumSize(100)
                        .initialCapacity(1)
                        .expireAfterAccess(120, TimeUnit.MINUTES)
                        .build(
                                new CacheLoader<CacheKey, WFSTemplate>() {
                                    @Override
                                    public WFSTemplate load(CacheKey key) {
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
                                        WFSTemplate template =
                                                new WFSTemplate(resource, namespaces);
                                        return template;
                                    }
                                });
    }

    /**
     * Get the template related to the featureType. If template has benn modified updates the cache
     * with the new JsonLdTemplate
     */
    public RootBuilder getTemplate(FeatureTypeInfo typeInfo, String outputFormat)
            throws ExecutionException {
        String fileName = getTemplateName(outputFormat);
        CacheKey key = new CacheKey(typeInfo, fileName);
        WFSTemplate template = templateCache.get(key);
        if (template.checkTemplate()) templateCache.put(key, template);
        boolean isValid;
        RootBuilder root = template.getRootBuilder();
        if (root != null) {
            TemplateValidator validator = new TemplateValidator(typeInfo);
            isValid = validator.validateTemplate(root);
            if (!isValid) {
                throw new RuntimeException(
                        "Failed to validate template for feature type "
                                + typeInfo.getName()
                                + ". Failing attribute is "
                                + URI.decode(validator.getFailingAttribute()));
            }
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

        public CacheKey(FeatureTypeInfo resource, String path) {
            this.resource = resource;
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

    private String getTemplateName(String outputFormat) {
        String templateName = "";
        if (outputFormat.equals(TemplateIdentifier.JSON.getOutputFormat()))
            templateName = TemplateIdentifier.JSON.getFilename();
        else if (outputFormat.equals(TemplateIdentifier.GEOJSON.getOutputFormat()))
            templateName = TemplateIdentifier.GEOJSON.getFilename();
        else if (outputFormat.equals(TemplateIdentifier.JSONLD.getOutputFormat())) {
            templateName = TemplateIdentifier.JSONLD.getFilename();
        }
        return templateName;
    }
}
