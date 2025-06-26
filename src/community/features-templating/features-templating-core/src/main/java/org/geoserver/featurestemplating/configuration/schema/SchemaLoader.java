/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.configuration.schema;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.builders.visitors.SimplifiedPropertyReplacer;
import org.geoserver.featurestemplating.configuration.AbstractLoader;
import org.geoserver.featurestemplating.configuration.TemplateIdentifier;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.data.complex.AppSchemaDataAccessRegistry;
import org.geotools.data.complex.DataAccessRegistry;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.feature.type.ComplexFeatureTypeImpl;
import org.geotools.data.complex.feature.type.Types;
import org.xml.sax.helpers.NamespaceSupport;

/** Manage the cache and the retrieving for all templates files */
public class SchemaLoader extends AbstractLoader {

    private final LoadingCache<CacheKey, SchemaDefinition> schemaCache;

    public SchemaLoader(GeoServerDataDirectory dd) {
        super(dd);
        schemaCache = CacheBuilder.newBuilder()
                .maximumSize(100)
                .initialCapacity(1)
                .expireAfterAccess(120, TimeUnit.MINUTES)
                .build(new SchemaCacheLoader());
    }

    /**
     * Get the template related to the featureType. Searching for the highest priority rule. If not found will try to
     * load it from the featureType directory as per legacy rule. If not found will return null.
     *
     * @param typeInfo the FeatureTypeInfo for which retrieve the template.
     * @param outputFormat the output format for which retrieve the template.
     * @param request the ows request can be null.
     * @return the RootBuilder.
     * @throws ExecutionException
     */
    public String getSchema(FeatureTypeInfo typeInfo, String outputFormat, Request request) throws ExecutionException {
        String schemaIdentifier =
                request == null ? evaluatesTemplateRule(typeInfo) : evaluatesTemplateRule(typeInfo, request);
        if (schemaIdentifier == null)
            schemaIdentifier = TemplateIdentifier.fromOutputFormat(outputFormat).getFilename();
        return getSchemaByIdentifier(typeInfo, schemaIdentifier);
    }

    /**
     * Get the template related to the featureType. Searching for the highest priority rule. If not found will try to
     * load it from the featureType directory as per legacy rule. If not found will return null.
     *
     * @param typeInfo the FeatureTypeInfo for which retrieve the template.
     * @param outputFormat the output format for which retrieve the template.
     * @return the RootBuilder.
     * @throws ExecutionException
     */
    public String getSchema(FeatureTypeInfo typeInfo, String outputFormat) throws ExecutionException {
        return getSchema(typeInfo, outputFormat, null);
    }

    private String getSchemaByIdentifier(FeatureTypeInfo typeInfo, String templateIdentifier)
            throws ExecutionException {
        CacheKey key = new CacheKey(typeInfo, templateIdentifier);
        SchemaDefinition schemaDefinition = schemaCache.get(key);
        boolean updateCache = false;
        if (schemaDefinition.checkSchema()) updateCache = true;

        String root = schemaDefinition.getSchemaContent();

        if (updateCache) {
            schemaCache.put(key, schemaDefinition);
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

    private void replaceSimplifiedPropertiesIfNeeded(FeatureTypeInfo featureTypeInfo, RootBuilder rootBuilder) {
        try {
            if (featureTypeInfo.getFeatureType() instanceof ComplexFeatureTypeImpl && rootBuilder != null) {

                DataAccessRegistry registry = AppSchemaDataAccessRegistry.getInstance();
                FeatureTypeMapping featureTypeMapping =
                        registry.mappingByElement(featureTypeInfo.getQualifiedNativeName());
                if (featureTypeMapping != null) {
                    SimplifiedPropertyReplacer visitor = new SimplifiedPropertyReplacer(featureTypeMapping);
                    rootBuilder.accept(visitor, null);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String evaluatesTemplateRule(FeatureTypeInfo featureTypeInfo) {
        return evaluatesTemplateRule(featureTypeInfo, Dispatcher.REQUEST.get());
    }

    // evaluates the template rule associated to the featureTypeInfo and return the TemplateInfo id.
    private String evaluatesTemplateRule(FeatureTypeInfo featureTypeInfo, Request request) {
        List<SchemaRule> matching = new ArrayList<>();
        SchemaRuleService ruleService = new SchemaRuleService(featureTypeInfo);
        Set<SchemaRule> rules = ruleService.getRules();
        String result = null;
        if (rules != null && !rules.isEmpty()) {
            for (SchemaRule r : rules) {
                if (r.applyRule(request)) matching.add(r);
            }
        }
        int size = matching.size();
        if (size > 0) result = getHighestPriorityIdentifier(matching);

        return result;
    }

    private String getHighestPriorityIdentifier(List<SchemaRule> rules) {
        if (rules.size() > 1) {
            SchemaRule.SchemaRuleComparator comparator = new SchemaRule.SchemaRuleComparator();
            rules.sort(comparator);
        }
        return rules.get(0).getSchemaIdentifier();
    }

    /**
     * Remove from the cache the entry with the specified identifier and Feature Type
     *
     * @param fti the FeatureType to which is associated the entry.
     * @param templateIdentifier the templateIdentifier of the cached template.
     */
    public void cleanCache(FeatureTypeInfo fti, String templateIdentifier) {
        CacheKey key = new CacheKey(fti, templateIdentifier);
        if (schemaCache.getIfPresent(key) != null) this.schemaCache.invalidate(key);
    }

    /**
     * Remove all the cached entries with the specified templateIdentifier.
     *
     * @param templateIdentifier the templateIdentifier used to identify the cache entries to remove.
     */
    public void removeAllWithIdentifier(String templateIdentifier) {
        Set<CacheKey> keys = schemaCache.asMap().keySet();
        for (CacheKey key : keys) {
            if (key.getIdentifier().equals(templateIdentifier)) {
                schemaCache.invalidate(key);
            }
        }
    }

    private SchemaFileManager getSchemaFileManager() {
        return SchemaFileManager.get();
    }

    private class SchemaCacheLoader extends CacheLoader<CacheKey, SchemaDefinition> {
        @Override
        public SchemaDefinition load(CacheKey key) {
            NamespaceSupport namespaces = null;
            try {
                FeatureType type = key.getResource().getFeatureType();
                namespaces = declareNamespaces(type);
            } catch (IOException e) {
                throw new RuntimeException("Error retrieving FeatureType "
                        + key.getResource().getName()
                        + "Exception is: "
                        + e.getMessage());
            }
            SchemaInfo schemaInfo = SchemaInfoDAO.get().findById(key.getIdentifier());
            Resource resource;
            if (schemaInfo != null) resource = getSchemaFileManager().getSchemaResource(schemaInfo);
            else resource = getDataDirectory().get(key.getResource(), key.getIdentifier());
            SchemaDefinition schemaDefinition = new SchemaDefinition(resource);
            return schemaDefinition;
        }
    }

    /** Invalidate all the cache entries. */
    public void reset() {
        schemaCache.invalidateAll();
    }

    public static SchemaLoader get() {
        return GeoServerExtensions.bean(SchemaLoader.class);
    }
}
