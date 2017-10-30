/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.versioning;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.GetFeatureCallback;
import org.geoserver.wfs.GetFeatureContext;
import org.geoserver.wfs.InsertElementHandler;
import org.geoserver.wfs.TransactionCallback;
import org.geoserver.wfs.TransactionContext;
import org.geoserver.wfs.TransactionContextBuilder;
import org.geoserver.wfs.request.Insert;
import org.geoserver.wfs.request.RequestObject;
import org.geoserver.wfs.request.Update;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.Converters;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class TimeVersioningCallback implements GetFeatureCallback, TransactionCallback {

    private static final FilterFactory2 FILTER_FACTORY = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    private final Catalog catalog;

    TimeVersioningCallback(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public GetFeatureContext beforeQuerying(GetFeatureContext context) {
        if (!isWfs20(context.getRequest())) {
            return context;
        }
        FeatureTypeInfo featureTypeInfo = context.getFeatureTypeInfo();
        if (!TimeVersioning.isEnabled(featureTypeInfo)) {
            // time versioning is not enabled for this feature type or is not a WFS 2.0 request
            return context;
        }
        VersioningFilterAdapter.adapt(featureTypeInfo, context.getQuery().getFilter());
        SortBy sort = FILTER_FACTORY.sort(TimeVersioning.getTimePropertyName(featureTypeInfo), SortOrder.DESCENDING);
        SortBy[] sorts = context.getQuery().getSortBy();
        if (sorts == null) {
            sorts = new SortBy[]{sort};
        } else {
            sorts = Arrays.copyOf(sorts, sorts.length + 1);
            sorts[sorts.length - 1] = sort;
        }
        context.getQuery().setSortBy(sorts);
        return context;
    }

    @Override
    public TransactionContext beforeHandlerExecution(TransactionContext context) {
        if (!isWfs20(context.getRequest())) {
            return context;
        }
        if (context.getElement() instanceof Update) {
            Insert insert = buildInsertForUpdate(context);
            InsertElementHandler handler = GeoServerExtensions.bean(InsertElementHandler.class);
            return new TransactionContextBuilder()
                    .withContext(context)
                    .withElement(insert)
                    .withHandler(handler).build();
        }
        if (context.getElement() instanceof Insert) {
            Insert insert = (Insert) context.getElement();
            for (Object element : insert.getFeatures()) {
                if (element instanceof SimpleFeature) {
                    setTimeAttribute((SimpleFeature) element);
                }
            }
        }
        return context;
    }

    @Override
    public TransactionContext beforeInsertFeatures(TransactionContext context) {
        return context;
    }

    @Override
    public TransactionContext beforeUpdateFeatures(TransactionContext context) {
        return context;
    }

    @Override
    public TransactionContext beforeDeleteFeatures(TransactionContext context) {
        return context;
    }

    @Override
    public TransactionContext beforeReplaceFeatures(TransactionContext context) {
        return context;
    }

    private void setTimeAttribute(SimpleFeature feature) {
        FeatureType featureType = feature.getFeatureType();
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(featureType);
        if (TimeVersioning.isEnabled(featureTypeInfo)) {
            String timePropertyName = TimeVersioning.getTimePropertyName(featureTypeInfo);
            AttributeDescriptor attributeDescriptor = feature.getType().getDescriptor(timePropertyName);
            Object timeValue = Converters.convert(new Date(), attributeDescriptor.getType().getBinding());
            feature.setAttribute(timePropertyName, timeValue);
        }
    }

    private SimpleFeatureCollection getTransactionFeatures(TransactionContext context) {
        QName typeName = context.getElement().getTypeName();
        Filter filter = context.getElement().getFilter();
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(new NameImpl(typeName));
        SimpleFeatureStore store = getTransactionStore(context);
        try {
            Query query = new Query();
            query.setFilter(VersioningFilterAdapter.adapt(featureTypeInfo, filter));
            SortBy sort = FILTER_FACTORY.sort(TimeVersioning.getTimePropertyName(featureTypeInfo), SortOrder.DESCENDING);
            query.setSortBy(new SortBy[]{sort});
            return store.getFeatures(query);
        } catch (Exception exception) {
            throw new RuntimeException(String.format(
                    "Error getting features of type '%s'.", typeName), exception);
        }
    }

    private Comparator<SimpleFeature> buildFeatureTimeComparator(FeatureTypeInfo featureTypeInfo) {
        String timePropertyName = TimeVersioning.getTimePropertyName(featureTypeInfo);
        return (featureA, featureB) -> {
            Date timeA = Converters.convert(featureA.getAttribute(timePropertyName), Date.class);
            Date timeB = Converters.convert(featureB.getAttribute(timePropertyName), Date.class);
            if (timeA == null) {
                return -1;
            }
            return timeA.compareTo(timeB);
        };
    }

    private List<SimpleFeature> getOnlyRecentFeatures(SimpleFeatureCollection features, FeatureTypeInfo featureTypeInfo) {
        String nameProperty = TimeVersioning.getNamePropertyName(featureTypeInfo);
        Map<Object, List<SimpleFeature>> featuresIndexedById = new HashMap<>();
        SimpleFeatureIterator iterator = features.features();
        while (iterator.hasNext()) {
            SimpleFeature feature = iterator.next();
            Object id = feature.getAttribute(nameProperty);
            List<SimpleFeature> existing = featuresIndexedById.computeIfAbsent(id, key -> new ArrayList<>());
            existing.add(feature);
        }
        Comparator<SimpleFeature> comparator = buildFeatureTimeComparator(featureTypeInfo);
        List<SimpleFeature> finalFeatures = new ArrayList<>();
        featuresIndexedById.values().forEach(indexed -> {
            indexed.sort(comparator);
            SimpleFeature feature = indexed.get(0);
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(feature.getFeatureType());
            builder.init(feature);
            finalFeatures.add(builder.buildFeature(null));
        });
        return finalFeatures;
    }

    private Insert buildInsertForUpdate(TransactionContext context) {
        Update update = (Update) context.getElement();
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(new NameImpl(update.getTypeName()));
        SimpleFeatureCollection features = getTransactionFeatures(context);
        List<SimpleFeature> recent = getOnlyRecentFeatures(features, featureTypeInfo);
        List<SimpleFeature> newFeatures = recent.stream()
                .map(this::prepareInsertFeature).collect(Collectors.toList());
        return new UpdateInsert(context.getRequest(), newFeatures);
    }

    private SimpleFeature prepareInsertFeature(SimpleFeature feature) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(feature.getFeatureType());
        builder.init(feature);
        SimpleFeature versionedFeature = builder.buildFeature(null);
        setTimeAttribute(versionedFeature);
        return versionedFeature;
    }

    private FeatureTypeInfo getFeatureTypeInfo(FeatureType featureType) {
        Name featureTypeName = featureType.getName();
        return getFeatureTypeInfo(featureTypeName);
    }

    private FeatureTypeInfo getFeatureTypeInfo(Name featureTypeName) {
        FeatureTypeInfo featureTypeInfo = catalog.getFeatureTypeByName(featureTypeName);
        if (featureTypeInfo == null) {
            throw new RuntimeException(String.format(
                    "Couldn't find feature type info ''%s.", featureTypeName));
        }
        return featureTypeInfo;
    }

    private SimpleFeatureStore getTransactionStore(TransactionContext context) {
        QName typeName = context.getElement().getTypeName();
        FeatureStore store = (FeatureStore) context.getFeatureStores().get(typeName);
        return DataUtilities.simple(store);
    }

    private boolean isWfs20(RequestObject request) {
        return true;
    }

    private static final class UpdateInsert extends Insert {

        private final List<SimpleFeature> features;

        protected UpdateInsert(RequestObject request, List<SimpleFeature> features) {
            super(request.getAdaptee());
            this.features = features;
        }

        @Override
        public List getFeatures() {
            return features;
        }
    }
}
