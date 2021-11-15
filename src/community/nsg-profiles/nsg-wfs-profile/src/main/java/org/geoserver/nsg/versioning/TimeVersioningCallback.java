/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.versioning;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.wfs.*;
import org.geoserver.wfs.request.*;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.Converters;
import org.geotools.util.factory.GeoTools;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

final class TimeVersioningCallback implements GetFeatureCallback, TransactionCallback {

    private static final FilterFactory2 FILTER_FACTORY =
            CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    private final Catalog catalog;

    TimeVersioningCallback(Catalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public void beforeQuerying(GetFeatureContext context) {
        String version = context.getRequest().getVersion();
        if (version == null || !version.startsWith("2.0")) {
            return;
        }
        FeatureTypeInfo featureTypeInfo = context.getFeatureTypeInfo();
        if (!TimeVersioning.isEnabled(featureTypeInfo)) {
            // time versioning is not enabled for this feature type or is not a WFS 2.0 request
            return;
        }
        Query query = new Query(context.getQuery());
        Filter adapted = VersioningFilterAdapter.adapt(featureTypeInfo, query.getFilter());
        query.setFilter(adapted);
        SortBy sort =
                FILTER_FACTORY.sort(
                        TimeVersioning.getTimePropertyName(featureTypeInfo), SortOrder.DESCENDING);
        SortBy[] sorts = query.getSortBy();
        if (sorts == null) {
            sorts = new SortBy[] {sort};
        } else {
            sorts = Arrays.copyOf(sorts, sorts.length + 1);
            sorts[sorts.length - 1] = sort;
        }
        query.setSortBy(sorts);
        context.setQuery(query);
    }

    @Override
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }

    private void setTimeAttribute(SimpleFeature feature, Date date) {
        FeatureType featureType = feature.getFeatureType();
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(featureType);
        if (TimeVersioning.isEnabled(featureTypeInfo)) {
            String timePropertyName = TimeVersioning.getTimePropertyName(featureTypeInfo);
            AttributeDescriptor attributeDescriptor =
                    feature.getType().getDescriptor(timePropertyName);
            Object timeValue = Converters.convert(date, attributeDescriptor.getType().getBinding());
            feature.setAttribute(timePropertyName, timeValue);
        }
    }

    private SimpleFeatureCollection getTransactionFeatures(Update update) throws IOException {
        QName typeName = update.getTypeName();
        Filter filter = update.getFilter();
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(new NameImpl(typeName));
        SimpleFeatureSource source = getTransactionSource(update);
        try {
            Query query = new Query();
            query.setFilter(VersioningFilterAdapter.adapt(featureTypeInfo, filter));
            SortBy sort =
                    FILTER_FACTORY.sort(
                            TimeVersioning.getTimePropertyName(featureTypeInfo),
                            SortOrder.DESCENDING);
            query.setSortBy(new SortBy[] {sort});
            return source.getFeatures(query);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Error getting features of type '%s'.", typeName), exception);
        }
    }

    /**
     * Returns the most recent version of each feature (note, this is an aggregate operator, a
     * visitor, wondering if it could be optimized in a single db query)
     */
    private List<SimpleFeature> getMostRecentFeatures(
            SimpleFeatureCollection timeSortedFeatures, FeatureTypeInfo featureTypeInfo) {
        String nameProperty = TimeVersioning.getNamePropertyName(featureTypeInfo);
        Map<Object, SimpleFeature> featuresIndexedById = new HashMap<>();
        try (SimpleFeatureIterator iterator = timeSortedFeatures.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Object id = feature.getAttribute(nameProperty);
                featuresIndexedById.putIfAbsent(id, feature);
            }
        }
        return new ArrayList<>(featuresIndexedById.values());
    }

    /**
     * Takes an update and transforms it in an insert if the feature type is versioned, leave it as
     * is otherwise
     */
    private TransactionElement transformUpdate(
            TransactionRequest request, Update update, Date referenceTime) throws IOException {
        FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(new NameImpl(update.getTypeName()));
        if (!TimeVersioning.isEnabled(featureTypeInfo)) {
            return update;
        }
        SimpleFeatureCollection features = getTransactionFeatures(update);
        List<SimpleFeature> recent = getMostRecentFeatures(features, featureTypeInfo);
        List<SimpleFeature> newFeatures =
                recent.stream()
                        .map(f -> prepareInsertFeature(f, update, referenceTime))
                        .collect(Collectors.toList());
        Insert insert = request.createInsert();
        insert.setHandle(update.getHandle());
        insert.getFeatures().addAll(newFeatures);

        return insert;
    }

    private SimpleFeature prepareInsertFeature(
            SimpleFeature feature, Update update, Date referenceTime) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(feature.getFeatureType());
        builder.init(feature);
        SimpleFeature versionedFeature = builder.buildFeature(null);
        // run the update
        for (Object o : update.getUpdateProperties()) {
            Property p = (Property) o;
            versionedFeature.setAttribute(p.getName().getLocalPart(), p.getValue());
        }
        // set the time
        setTimeAttribute(versionedFeature, referenceTime);
        return versionedFeature;
    }

    private FeatureTypeInfo getFeatureTypeInfo(FeatureType featureType) {
        Name featureTypeName = featureType.getName();
        return getFeatureTypeInfo(featureTypeName);
    }

    private FeatureTypeInfo getFeatureTypeInfo(Name featureTypeName) {
        FeatureTypeInfo featureTypeInfo = catalog.getFeatureTypeByName(featureTypeName);
        if (featureTypeInfo == null) {
            throw new RuntimeException(
                    String.format("Couldn't find feature type info ''%s.", featureTypeName));
        }
        return featureTypeInfo;
    }

    private SimpleFeatureSource getTransactionSource(Update update) throws IOException {
        QName typeName = update.getTypeName();

        final String name = typeName.getLocalPart();
        final String namespaceURI;

        if (typeName.getNamespaceURI() != null) {
            namespaceURI = typeName.getNamespaceURI();
        } else {
            namespaceURI = catalog.getDefaultNamespace().getURI();
        }

        final FeatureTypeInfo meta = catalog.getFeatureTypeByName(namespaceURI, name);

        if (meta == null) {
            String msg = "Feature type '" + name + "' is not available: ";
            throw new WFSTransactionException(msg, (String) null, update.getHandle());
        }

        FeatureSource source = meta.getFeatureSource(null, null);
        return DataUtilities.simple(source);
    }

    @Override
    public TransactionRequest beforeTransaction(TransactionRequest request) throws WFSException {
        if (request.getVersion() == null || !request.getVersion().startsWith("2.0")) {
            return request;
        }

        // all changes in this transaction will carry the same reference time
        Date referenceTime = new Date();
        List<TransactionElement> newElements = new ArrayList<>();

        for (TransactionElement element : request.getElements()) {
            if (element instanceof Insert) {
                for (Object of : ((Insert) element).getFeatures()) {
                    if (of instanceof SimpleFeature) {
                        setTimeAttribute((SimpleFeature) of, referenceTime);
                    }
                }
                newElements.add(element);
            } else if (element instanceof Update) {
                try {
                    TransactionElement transformed =
                            transformUpdate(request, (Update) element, referenceTime);
                    newElements.add(transformed);
                } catch (IOException e) {
                    throw new WFSException(e);
                }
            } else if (element instanceof Delete) {
                Delete delete = (Delete) element;
                FeatureTypeInfo featureTypeInfo =
                        getFeatureTypeInfo(new NameImpl(delete.getTypeName()));
                if (TimeVersioning.isEnabled(featureTypeInfo)) {
                    Filter filter = delete.getFilter();
                    Filter adaptedFilter = VersioningFilterAdapter.adapt(featureTypeInfo, filter);
                    delete.setFilter(adaptedFilter);
                }
                newElements.add(delete);
            } else if (element instanceof Replace) {
                newElements.add(element);
            }
        }
        request.setElements(newElements);

        return request;
    }

    @Override
    public void beforeCommit(TransactionRequest request) throws WFSException {
        // nothing to do
    }

    @Override
    public void afterTransaction(
            TransactionRequest request, TransactionResponse result, boolean committed) {
        // nothing to do
    }

    @Override
    public void dataStoreChange(TransactionEvent event) throws WFSException {
        // nothing to do
    }
}
