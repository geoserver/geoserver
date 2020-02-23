/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.GeoServer;
import org.geoserver.csw.CSWInfo;
import org.geoserver.csw.DirectDownloadSettings;
import org.geoserver.csw.feature.sort.CatalogComparatorFactory;
import org.geoserver.csw.records.GenericRecordBuilder;
import org.geoserver.csw.records.RecordBuilder;
import org.geoserver.csw.records.RecordDescriptor;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;

/**
 * Internal Catalog Store Feature Iterator
 *
 * @author Niels Charlier
 */
class CatalogStoreFeatureIterator implements Iterator<Feature> {

    protected static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    static final Logger LOGGER = Logging.getLogger(CatalogStoreFeatureIterator.class);

    protected RecordBuilder builder;

    protected Iterator<ResourceInfo> layerIt;

    protected ResourceInfo nextResource;

    protected Iterator<LayerGroupInfo> layerGroupIt;

    protected LayerGroupInfo nextLayerGroup;

    protected CatalogStoreMapping mapping;

    protected CatalogFacade catalogFacade;

    protected Map<String, String> interpolationProperties = new HashMap<String, String>();

    protected int offset;

    protected int count;

    protected SortBy[] sortOrder;

    protected Filter filter;

    protected int index;

    protected Comparator<Info> comparator;

    private RecordDescriptor recordDescriptor;

    public CatalogStoreFeatureIterator(
            int offset,
            int count,
            SortBy[] sortOrder,
            Filter filter,
            Catalog catalog,
            CatalogStoreMapping mapping,
            RecordDescriptor recordDescriptor,
            Map<String, String> interpolationProperties) {
        this.interpolationProperties = interpolationProperties;
        this.offset = offset;
        this.count = count;
        this.sortOrder = sortOrder;
        this.filter = filter;
        catalogFacade = catalog.getFacade();
        this.mapping = mapping;

        Filter advertised = ff.equals(ff.property("advertised"), ff.literal(true));

        layerIt =
                catalogFacade.list(
                        ResourceInfo.class, ff.and(filter, advertised), null, null, sortOrder);
        nextLayer();
        layerGroupIt = catalogFacade.list(LayerGroupInfo.class, filter, null, null, sortOrder);
        nextLayerGroup();

        comparator =
                sortOrder == null || sortOrder.length == 0
                        ? null
                        : CatalogComparatorFactory.buildComparator(sortOrder);
        index = 0;
        while (index < offset && hasNext()) {
            nextInternal();
        }
        this.recordDescriptor = recordDescriptor;
        builder = new GenericRecordBuilder(recordDescriptor);
    }

    @Override
    public boolean hasNext() {
        return index < offset + count && (nextResource != null || nextLayerGroup != null);
    }

    public ResourceInfo nextLayer() {
        ResourceInfo result = nextResource;

        if (layerIt.hasNext()) {
            nextResource = layerIt.next();
        } else {
            nextResource = null;
        }

        return result;
    }

    public LayerGroupInfo nextLayerGroup() {
        LayerGroupInfo result = nextLayerGroup;

        if (layerGroupIt.hasNext()) {
            nextLayerGroup = layerGroupIt.next();
        } else {
            nextLayerGroup = null;
        }

        return result;
    }

    public CatalogInfo nextInternal() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more records to retrieve");
        }
        index++;

        if (nextResource == null) {
            return nextLayerGroup();
        }

        if (nextLayerGroup == null) {
            return nextLayer();
        }

        if (comparator == null) {
            return nextLayer();
        }

        int c = comparator.compare(nextResource, nextLayerGroup);
        if (c <= 0) {
            return nextLayer();
        } else {
            return nextLayerGroup();
        }
    }

    @Override
    public Feature next() {
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
            Object value = mappingElement.getContent().evaluate(resource);

            if (value != null) {
                if (value instanceof Collection) {
                    ((Collection) value).removeAll(Collections.singleton(null));
                    if (((Collection) value).size() > 0) {
                        String[] elements = new String[((Collection) value).size()];
                        int i = 0;
                        for (Object element : (Collection) value) {
                            elements[i++] =
                                    interpolate(interpolationProperties, element.toString());
                        }
                        builder.addElement(
                                mappingElement.getKey(), mappingElement.getSplitIndex(), elements);
                    }
                } else {
                    builder.addElement(
                            mappingElement.getKey(),
                            interpolate(interpolationProperties, value.toString()));
                }

                if (mappingElement == mapping.getIdentifierElement()) {
                    id = interpolate(interpolationProperties, value.toString());
                }
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
                String typeName = recordDescriptor.getFeatureType().getName().getLocalPart();
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
    }

    private Feature convertToFeature(LayerGroupInfo resource) {

        String id = mapProperties(resource);

        // move on to the bounding boxes
        if (mapping.isIncludeEnvelope()) {
            ReferencedEnvelope bbox = null;
            bbox = resource.getBounds();
            if (bbox != null) {
                builder.addBoundingBox(bbox);
            }
        }

        return builder.build(id);
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
            String propertyValue = (String) properties.get(propertyName);
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
}
