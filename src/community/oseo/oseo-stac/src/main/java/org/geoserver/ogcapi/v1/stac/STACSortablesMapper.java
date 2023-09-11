/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2021, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.geoserver.ogcapi.QueryablesBuilder.getSchema;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.featurestemplating.builders.TemplateBuilder;
import org.geoserver.featurestemplating.builders.TemplateBuilderUtils;
import org.geoserver.featurestemplating.builders.impl.DynamicValueBuilder;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ogcapi.Queryables;
import org.geoserver.ogcapi.Sortables;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.visitor.ExpressionTypeVisitor;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;

/**
 * Maps properties in the source template to public sortables. For a property to be sortable it
 * must:
 *
 * <ul>
 *   <li>Map to a single source property, without expressions
 *   <li>Be of a simple comparable type (strings, numbers, dates)
 * </ul>
 */
public class STACSortablesMapper {

    static final Logger LOGGER = Logging.getLogger(STACSortablesMapper.class);

    private final TemplateBuilder template;
    private final FeatureType itemsSchema;
    private final String id;
    private final STACQueryablesBuilder queryablesBuilder;

    /**
     * Generates Sortables for a given template
     *
     * @param template the template to generate sortables for
     * @param itemsSchema the schema of the items in the collection
     * @param queryablesBuilder the queryables builder to use get a configurable list of attributes
     */
    public STACSortablesMapper(
            TemplateBuilder template,
            FeatureType itemsSchema,
            STACQueryablesBuilder queryablesBuilder) {
        this(template, itemsSchema, null, queryablesBuilder);
    }

    /**
     * Generates Sortables for a given template
     *
     * @param template the template to generate sortables for
     * @param itemsSchema the schema of the items in the collection
     * @param id the id of the sortables document
     * @param queryablesBuilder the queryables builder to use get a configurable list of attributes
     */
    public STACSortablesMapper(
            TemplateBuilder template,
            FeatureType itemsSchema,
            String id,
            STACQueryablesBuilder queryablesBuilder) {
        this.id = id;
        this.template = template;
        this.itemsSchema = itemsSchema;
        this.queryablesBuilder = queryablesBuilder;
    }

    /**
     * Returns the sortables for the template
     *
     * @param collectionId the collection id to use to filter the queryables
     * @param templates the templates to use to filter the queryables
     * @param sampleFeatures the sample features to use to filter the queryables
     * @param collectionsCache the collections cache to use to filter the queryables
     * @param itemsSchema the items schema to use to filter the queryables
     * @param geoServer the GeoServer instance to use to filter the queryables
     * @param itemTemplate the item template to use to filter the queryables
     * @param id the id of the sortables document
     * @return the sortables for the template
     * @throws IOException if an error occurs while reading the queryables
     */
    public static STACSortablesMapper getSortablesMapper(
            String collectionId,
            STACTemplates templates,
            SampleFeatures sampleFeatures,
            CollectionsCache collectionsCache,
            FeatureType itemsSchema,
            GeoServer geoServer,
            TemplateBuilder itemTemplate,
            String id)
            throws IOException {
        STACQueryablesBuilder stacQueryablesBuilder =
                new STACQueryablesBuilder(
                        null,
                        templates.getItemTemplate(collectionId),
                        sampleFeatures.getSchema(),
                        sampleFeatures.getSample(collectionId),
                        collectionsCache.getCollection(collectionId),
                        geoServer.getService(OSEOInfo.class));
        return new STACSortablesMapper(itemTemplate, itemsSchema, id, stacQueryablesBuilder);
    }
    /**
     * Builds the SortablesMapper for the STAC API
     *
     * @param collectionId the collection id
     * @param templates the templates
     * @param sampleFeatures the sample features
     * @param collectionsCache the collections cache
     * @param itemsSchema the items schema
     * @param geoServer the GeoServer instance
     * @return the SortablesMapper
     * @throws IOException if an error occurs while reading the templates
     */
    public static STACSortablesMapper getSortablesMapper(
            String collectionId,
            STACTemplates templates,
            SampleFeatures sampleFeatures,
            CollectionsCache collectionsCache,
            FeatureType itemsSchema,
            GeoServer geoServer)
            throws IOException {
        STACQueryablesBuilder stacQueryablesBuilder =
                new STACQueryablesBuilder(
                        null,
                        templates.getItemTemplate(collectionId),
                        sampleFeatures.getSchema(),
                        sampleFeatures.getSample(collectionId),
                        collectionsCache.getCollection(collectionId),
                        geoServer.getService(OSEOInfo.class));
        TemplateBuilder builder = templates.getItemTemplate(collectionId);
        return new STACSortablesMapper(builder, itemsSchema, stacQueryablesBuilder);
    }
    /**
     * Returns the sortables document for the template
     *
     * @return the sortables document
     * @throws IOException if an error occurs while reading the template
     */
    public Sortables getSortables() throws IOException {
        Sortables result = new Sortables(id);
        Queryables queryables = queryablesBuilder.getQueryables();
        // force in the extra well known sortables
        result.setProperties(new LinkedHashMap<>());
        result.getProperties().put("id", getSchema(String.class));
        result.getProperties().put("collection", getSchema(String.class));
        result.getProperties().put("datetime", getSchema(Date.class));

        TemplateBuilder properties =
                TemplateBuilderUtils.getBuilderFor(template, "features", "properties");
        if (properties != null) {
            TemplatePropertyVisitor visitor =
                    new TemplatePropertyVisitor(
                            properties,
                            null,
                            (path, vb) -> {
                                if (isSortable(vb) && queryables.getProperties().containsKey(path))
                                    result.getProperties().put(path, getSchema(getClass(vb)));
                            });
            visitor.visit();
        }

        return result;
    }

    /** Maps sortable properties to source properties */
    public Map<String, String> getSortablesMap() throws IOException {
        Queryables queryables = queryablesBuilder.getQueryables();
        Map<String, String> result = new HashMap<>();
        TemplateBuilder properties =
                TemplateBuilderUtils.getBuilderFor(template, "features", "properties");
        FilterAttributeExtractor extractor = new FilterAttributeExtractor();
        if (properties != null) {
            TemplatePropertyVisitor visitor =
                    new TemplatePropertyVisitor(
                            properties,
                            null, // no sample feature, cannot sort on a jsonPointer anyways
                            (path, vb) -> {
                                if (isSortable(vb)
                                        && queryables.getProperties().containsKey(path)) {
                                    if (vb.getXpath() != null) {
                                        result.put(path, vb.getXpath().getPropertyName());
                                    } else if (vb.getCql() != null) {
                                        vb.getCql().accept(extractor, null);
                                        Set<PropertyName> propertiesNames =
                                                extractor.getPropertyNameSet();
                                        // if there is more than one property name in the
                                        // expression, we cannot determine which to use
                                        if (propertiesNames.size() == 1) {
                                            result.put(
                                                    path,
                                                    propertiesNames
                                                            .iterator()
                                                            .next()
                                                            .getPropertyName());
                                        } else {
                                            LOGGER.log(
                                                    Level.WARNING,
                                                    "Cannot determine the property name to use for sorting on "
                                                            + path
                                                            + " because the expression "
                                                            + vb.getCql()
                                                            + " contains more than one property name");
                                        }
                                    }
                                }
                            });
            visitor.visit();
        }
        // force in the extra well known sortables
        result.put("collection", "parentIdentifier");
        result.put("datetime", "timeStart");
        result.put("id", "identifier");

        return result;
    }

    private Class getClass(DynamicValueBuilder db) {
        AttributeExpressionImpl xpath = db.getXpath();
        if (xpath != null) {
            if (!xpath.getPropertyName().contains("/")) {
                Object result = xpath.evaluate(itemsSchema);
                if (result instanceof PropertyDescriptor) {
                    PropertyDescriptor pd = (PropertyDescriptor) result;
                    return pd.getType().getBinding();
                }
            }
        } else if (db.getCql() != null) {
            ExpressionTypeVisitor visitor = new ExpressionTypeVisitor(itemsSchema);
            return (Class<?>) db.getCql().accept(visitor, null);
        }
        return null;
    }

    private boolean isSortable(DynamicValueBuilder db) {
        Class<?> binding = getClass(db);
        if (binding == null) return false;
        return (Number.class.isAssignableFrom(binding)
                || Date.class.isAssignableFrom(binding)
                || String.class.isAssignableFrom(binding));
    }

    /** Maps a SortBy[] using public sortables back to source property names */
    public SortBy[] map(SortBy[] sortby) throws IOException {
        Map<String, String> sortables = getSortablesMap();
        return Arrays.stream(sortby)
                .map(sb -> mapSortable(sb, sortables))
                .toArray(n -> new SortBy[n]);
    }

    private Object mapSortable(SortBy sb, Map<String, String> sortables) {
        String sortable = sb.getPropertyName().getPropertyName();
        String sourceName = sortables.get(sortable);
        if (sourceName == null)
            throw new APIException(
                    APIException.INVALID_PARAMETER_VALUE,
                    "Unknown sortable: " + sortable,
                    HttpStatus.NOT_FOUND);
        return STACService.FF.sort(sourceName, sb.getSortOrder());
    }
}
