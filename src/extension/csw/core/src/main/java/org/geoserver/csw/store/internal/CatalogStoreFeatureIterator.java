/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.ObjectUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.CSWInfo;
import org.geoserver.csw.DirectDownloadSettings;
import org.geoserver.csw.records.GenericRecordBuilder;
import org.geoserver.csw.records.RecordBuilder;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.api.feature.Feature;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;

/**
 * Internal Catalog Store Feature Iterator
 *
 * @author Niels Charlier
 */
class CatalogStoreFeatureIterator implements Iterator<Feature> {

    static final Logger LOGGER = Logging.getLogger(CatalogStoreFeatureIterator.class);

    protected RecordBuilder builder;

    protected Iterator<? extends PublishedInfo> it;

    protected CatalogInfo next = null;

    protected CatalogStoreMapping mapping;

    protected CatalogFacade catalogFacade;

    protected Map<String, String> interpolationProperties = new HashMap<>();

    protected SortBy[] sortOrder;

    protected Filter filter;

    protected int index;

    private RecordDescriptor outputRecordDescriptor;

    public CatalogStoreFeatureIterator(
            int offset,
            int count,
            SortBy[] sortOrder,
            Filter filter,
            Catalog catalog,
            CatalogStoreMapping mapping,
            RecordDescriptor outputRecordDescriptor,
            Map<String, String> interpolationProperties) {
        this.interpolationProperties = interpolationProperties;
        this.sortOrder = sortOrder;
        this.filter = filter;
        catalogFacade = catalog.getFacade();
        this.mapping = mapping;

        it = catalogFacade.list(PublishedInfo.class, filter, offset, count, sortOrder);

        nextInternal();

        this.outputRecordDescriptor = outputRecordDescriptor;
        builder = new GenericRecordBuilder(outputRecordDescriptor);
    }

    @Override
    public boolean hasNext() {
        return next != null;
    }

    public CatalogInfo nextInternal() {

        CatalogInfo result = next;

        if (it.hasNext()) {
            next = it.next();
            if (next instanceof LayerInfo) {
                next = ((LayerInfo) next).getResource();
            }
        } else {
            next = null;
        }

        return result;
    }

    @Override
    public Feature next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more records to retrieve");
        }

        CatalogInfo info = nextInternal();

        if (info instanceof ResourceInfo) {
            return convertToFeature((ResourceInfo) info);
        } else {
            return convertToFeature((LayerGroupInfo) info);
        }
    }

    private String mapProperties(CatalogInfo resource) {
        String id = null;
        for (CatalogStoreMapping.CatalogStoreMappingElement mappingElement : mapping.elements()) {
            Object value;
            try {
                value = mappingElement.getContent().evaluate(resource);

                if (value != null || mappingElement.isRequired()) {
                    if (value instanceof Collection) {
                        List<Object> elements =
                                interpolate(interpolationProperties, (Collection<?>) value);
                        if (elements != null) {
                            builder.addElement(
                                    mappingElement.getKey(),
                                    mappingElement.getSplitIndex(),
                                    elements.toArray(new Object[elements.size()]));
                        }
                    } else {
                        builder.addElement(
                                mappingElement.getKey(),
                                interpolate(interpolationProperties, ObjectUtils.toString(value)));
                    }

                    if (mappingElement == mapping.getIdentifierElement()) {
                        id = interpolate(interpolationProperties, ObjectUtils.toString(value));
                    }
                }

            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Failed mapping property '"
                                + mappingElement.getKey()
                                + "': "
                                + e.getMessage(),
                        e);
            }
        }
        return id;
    }

    /** Get a {@link FeatureCustomizer} for this info. */
    private FeatureCustomizer getCustomizer(CatalogInfo info) {
        FeatureCustomizer customizer = null;

        // DirectDownload capability is only checked for Coverage layers
        if (info instanceof CoverageInfo) {
            CoverageInfo coverageInfo = ((CoverageInfo) info);
            MetadataMap metadata = coverageInfo.getMetadata();

            boolean directDownloadEnabled = false;
            // Look for specific settings for this layer
            DirectDownloadSettings settings =
                    DirectDownloadSettings.getSettingsFromMetadata(
                            metadata,
                            GeoServerExtensions.bean(GeoServer.class).getService(CSWInfo.class));
            if (settings != null) {
                directDownloadEnabled = settings.isDirectDownloadEnabled();
            }

            if (directDownloadEnabled) {
                String typeName = outputRecordDescriptor.getFeatureType().getName().getLocalPart();
                // customizer = FeatureCustomizer.getCustomizer(typeName);
                customizer = FeatureCustomizer.getCustomizer(typeName);
                if (customizer == null) {
                    if (LOGGER.isLoggable(Level.WARNING)) {
                        LOGGER.warning(
                                "No Mapping customizer have been found for "
                                        + typeName
                                        + ". Mapping customizations will not be made");
                    }
                }
            }
        }
        return customizer;
    }

    private Feature convertToFeature(ResourceInfo resource) {
        try {
            String id = mapProperties(resource);

            // move on to the bounding boxes
            if (mapping.isIncludeEnvelope()) {
                ReferencedEnvelope bbox = null;
                try {
                    bbox = resource.boundingBox();
                } catch (Exception e) {
                    LOGGER.log(Level.INFO, "Failed to parse original record bbox");
                }
                if (bbox != null) {
                    builder.addBoundingBox(bbox);
                }
            }
            Feature feature = builder.build(id);
            FeatureCustomizer customizer = getCustomizer(resource);
            if (customizer != null) {
                customizer.customizeFeature(feature, ModificationProxy.unwrap(resource));
            }
            return feature;
        } catch (IllegalArgumentException e) {
            String message = "Error mapping resource " + resource.getName() + ": " + e.getMessage();
            LOGGER.log(Level.SEVERE, message, e);
            throw new IllegalArgumentException(message, e);
        }
    }

    private Feature convertToFeature(LayerGroupInfo resource) {
        try {
            String id = mapProperties(resource);

            // move on to the bounding boxes
            if (mapping.isIncludeEnvelope()) {
                ReferencedEnvelope bbox = resource.getBounds();
                if (bbox != null) {
                    builder.addBoundingBox(bbox);
                }
            }

            return builder.build(id);
        } catch (IllegalArgumentException e) {
            String message =
                    "Error mapping layer group " + resource.getName() + ": " + e.getMessage();
            LOGGER.log(Level.SEVERE, message, e);
            throw new IllegalArgumentException(message, e);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This iterator is read only");
    }

    /** Pattern to match a property to be substituted. Note the reluctant quantifier. */
    protected static final Pattern PROPERTY_INTERPOLATION_PATTERN =
            Pattern.compile("\\$\\{(.+?)\\}");

    protected static String interpolate(Map<String, String> properties, String input) {
        String result = input;
        Matcher matcher = PROPERTY_INTERPOLATION_PATTERN.matcher(result);
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            String propertyValue = properties.get(propertyName);
            if (propertyValue == null) {
                throw new RuntimeException(
                        "Interpolation failed for missing property " + propertyName);
            } else {
                result = result.replace(matcher.group(), propertyValue).trim();
                matcher.reset(result);
            }
        }
        return result;
    }

    protected static List<Object> interpolate(Map<String, String> properties, Collection<?> value) {
        if (!value.isEmpty()) {
            List<Object> elements = new ArrayList<>();
            for (Object element : value) {
                Object result = null;
                if (element instanceof Collection<?>) {
                    result = interpolate(properties, (Collection<?>) element);
                } else if (element != null) {
                    result = interpolate(properties, element.toString());
                }
                elements.add(result);
            }
            return elements;
        }
        return null;
    }
}
