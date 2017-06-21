/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.response.LinkFeatureComparator;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.opensearch.eo.store.OpenSearchAccess.ProductClass;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.BaseSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.FilterFactory2;
import org.springframework.http.HttpStatus;

/**
 * Base class for OpenSearch related REST controllers
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractOpenSearchController extends RestBaseController {

    static final String SOURCE_NAME = "SourceName";

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    protected OpenSearchAccessProvider accessProvider;

    public AbstractOpenSearchController(OpenSearchAccessProvider accessProvider) {
        this.accessProvider = accessProvider;
    }

    protected OpenSearchAccess getOpenSearchAccess() throws IOException {
        return accessProvider.getOpenSearchAccess();
    }

    protected void validateMin(Integer value, int min, String name) {
        if (value != null && value < min) {
            throw new RestException("Invalid parameter " + name + ", should be at least " + min,
                    HttpStatus.BAD_REQUEST);
        }
    }

    protected void validateMax(Integer value, int max, String name) {
        if (value != null && value > max) {
            throw new RestException("Invalid parameter " + name + ", should be at most " + max,
                    HttpStatus.BAD_REQUEST);
        }
    }

    protected void setupQueryPaging(Query query, Integer offset, Integer limit) {
        if (offset != null) {
            validateMin(offset, 0, "offset");
            query.setStartIndex(offset);
        }
        final int maximumRecordsPerPage = accessProvider.getService().getMaximumRecordsPerPage();
        if (limit != null) {
            validateMin(limit, 0, "limit");
            validateMax(limit, maximumRecordsPerPage, "limit");
            query.setMaxFeatures(limit);
        } else {
            query.setMaxFeatures(maximumRecordsPerPage);
        }
    }

    protected FeatureCollection<FeatureType, Feature> queryCollections(Query query)
            throws IOException {
        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        FeatureSource<FeatureType, Feature> fs = access.getCollectionSource();
        FeatureCollection<FeatureType, Feature> fc = fs.getFeatures(query);
        return fc;
    }

    protected Feature queryCollection(String collectionName, Consumer<Query> queryDecorator)
            throws IOException {
        Query query = new Query();
        query.setFilter(FF.equal(FF.property("name"), FF.literal(collectionName), true));
        queryDecorator.accept(query);
        FeatureCollection<FeatureType, Feature> fc = queryCollections(query);
        Feature feature = DataUtilities.first(fc);
        if (feature == null) {
            throw new ResourceNotFoundException(
                    "Could not find a collection named '" + collectionName + "'");
        }

        return feature;
    }

    protected SimpleFeatureType mapFeatureTypeToSimple(FeatureType schema,
            Consumer<SimpleFeatureTypeBuilder> extraAttributeBuilder) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            // skip multivalue, metadata and quicklook
            final Name propertyName = pd.getName();
            if (pd.getMaxOccurs() > 1
                    || OpenSearchAccess.METADATA_PROPERTY_NAME.equals(propertyName)
                    || OpenSearchAccess.QUICKLOOK_PROPERTY_NAME.equals(propertyName)
                    || "htmlDescription".equalsIgnoreCase(propertyName.getLocalPart())
                    || "id".equals(propertyName.getLocalPart())) {
                continue;
            }
            Name name = propertyName;
            String uri = name.getNamespaceURI();
            String prefix = null;
            if (OpenSearchAccess.EO_NAMESPACE.equals(uri)) {
                prefix = "eo";
            } else {
                for (ProductClass pc : ProductClass.values()) {
                    if (pc.getNamespace().equals(uri)) {
                        prefix = pc.getPrefix();
                        break;
                    }
                }
            }
            String mappedName;
            if (prefix != null) {
                mappedName = prefix + ":" + name.getLocalPart();
            } else {
                mappedName = name.getLocalPart();
            }
            tb.userData(SOURCE_NAME, name);
            tb.add(mappedName, pd.getType().getBinding());
        }
        tb.setName(schema.getName());
        extraAttributeBuilder.accept(tb);
        SimpleFeatureType targetSchema = tb.buildFeatureType();
        return targetSchema;
    }
    
    protected OgcLinks buildOgcLinksFromFeature(Feature feature) {
        // map to a list of beans
        List<OgcLink> links = Collections.emptyList();
        Collection<Property> linkProperties = feature
                .getProperties(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME);
        if (linkProperties != null) {
            links = linkProperties.stream().map(p -> (SimpleFeature) p)
                    .sorted(LinkFeatureComparator.INSTANCE).map(sf -> {
                        String offering = (String) sf.getAttribute("offering");
                        String method = (String) sf.getAttribute("method");
                        String code = (String) sf.getAttribute("code");
                        String type = (String) sf.getAttribute("type");
                        String href = (String) sf.getAttribute("href");
                        return new OgcLink(offering, method, code, type, href);
                    }).collect(Collectors.toList());
        }
        return new OgcLinks(links);
    }

    protected SimpleFeature mapFeatureToSimple(Feature f, SimpleFeatureType targetSchema,
            Consumer<SimpleFeatureBuilder> extraValueBuilder) {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetSchema);
        List<AttributeDescriptor> attributeDescriptors = targetSchema.getAttributeDescriptors();
        String identifier = f.getIdentifier().getID();
        for (AttributeDescriptor ad : attributeDescriptors) {
            Name sourceName = (Name) ad.getUserData().get(SOURCE_NAME);
            Property p = f.getProperty(sourceName);
            if (p != null) {
                Object value = p.getValue();
                if (value != null) {
                    fb.set(ad.getLocalName(), value);
                    if (("eo:identifier".equals(ad.getLocalName())
                            || "eop:identifier".equals(ad.getLocalName()))
                            && value instanceof String) {
                        identifier = (String) value;
                    }
                }
            }
        }
        extraValueBuilder.accept(fb);

        return fb.buildFeature(identifier);
    }

    /**
     * Un-maps OSEO related attributes to a prefix:name for for json encoding
     * 
     * @param fc
     * @return
     */
    protected SimpleFeatureCollection toSimpleFeatureCollection(
            FeatureCollection<FeatureType, Feature> fc,
            Consumer<SimpleFeatureTypeBuilder> extraAttributeBuilder,
            Consumer<SimpleFeatureBuilder> extraValuesBuilder) {
        SimpleFeatureType targetSchema = mapFeatureTypeToSimple(fc.getSchema(),
                extraAttributeBuilder);

        return new BaseSimpleFeatureCollection(targetSchema) {

            @Override
            public SimpleFeatureIterator features() {
                FeatureIterator<Feature> features = fc.features();
                return new SimpleFeatureIterator() {

                    @Override
                    public SimpleFeature next() throws NoSuchElementException {
                        Feature f = features.next();
                        return mapFeatureToSimple(f, targetSchema, extraValuesBuilder);
                    }

                    @Override
                    public boolean hasNext() {
                        return features.hasNext();
                    }

                    @Override
                    public void close() {
                        features.close();
                    }
                };
            }
        };
    }

}