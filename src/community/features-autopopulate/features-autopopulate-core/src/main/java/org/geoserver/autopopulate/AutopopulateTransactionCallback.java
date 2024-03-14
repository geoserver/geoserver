/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.autopopulate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wfs.*;
import org.geoserver.wfs.request.*;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.Filter;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;

/**
 * Listens to transactions (so far only issued by WFS), and autopopulates the feature type
 * attributes according to the values retrieved from the properties file.
 *
 * <p>A Spring bean singleton of this class needs to be declared in order for GeoServer transactions
 * to pick it up automatically and forward transaction events to it.
 *
 * <p>The plugin uses a custom TransactionCallback that alters the insert/update WFS-T operations,
 * forcing in specific values into them, based on configuration files.
 *
 * <p>To support configuration for multiple layers, the easiest thing is to place a configuration,
 * file in the directories of the layers themselves, pretty much like the featureinfo templates.
 *
 * <p>A "transactionCustomizer.properties" file that contains a set of names and CQL expressions
 * e.g.:
 *
 * <pre>
 * UTENTE=env('GSUSER') # this will be replaced with the current user see @EnviromentInjectionCallback
 * AGGIORNAMENTO=now()  # this will be replaced with the current date
 * </pre>
 *
 * <p>To keep things simple, the expressions will just use environment variables, but not see the
 * other values provided in the update/insert, and will not be differentiated by insert/update
 * cases.
 *
 * @author Alessio Fabiani, GeoSolutions SRL, alessio.fabiani@geosolutionsgroup.com
 * @see TransactionCallback
 */
public class AutopopulateTransactionCallback implements TransactionCallback {

    public static final String TRANSACTION_CUSTOMIZER_PROPERTIES =
            "transactionCustomizer.properties";
    /** logger */
    private static final Logger log = Logging.getLogger(AutopopulateTransactionCallback.class);
    /** The GeoServer catalog */
    private final Catalog catalog;

    public Map<TemplateKey, AutopopulateTemplate> getTemplateCache() {
        return templateCache;
    }

    public void setTemplateCache(Map<TemplateKey, AutopopulateTemplate> templateCache) {
        this.templateCache = templateCache;
    }

    /** Template cache used to avoid paying the cost of template lookup for each feature */
    Map<TemplateKey, AutopopulateTemplate> templateCache = new HashMap<>();

    public AutopopulateTransactionCallback(Catalog catalog) {
        this.catalog = catalog;
        log.info("AutopopulateTransactionCallback initialized");
    }

    /**
     * Not used, we're interested in the {@link #beforeTransaction} hook
     *
     * @see TransactionCallback#beforeCommit(TransactionRequest)
     */
    @Override
    public void beforeCommit(TransactionRequest request) throws WFSException {
        // nothing to do
    }

    /**
     * Not used, we're interested in the {@link #beforeTransaction} hook
     *
     * @see TransactionCallback#dataStoreChange(TransactionEvent)
     */
    @Override
    public void dataStoreChange(final TransactionEvent event) throws WFSException {
        // nothing to do
    }

    /**
     * Not used, we're interested in the {@link #beforeTransaction} hook
     *
     * @see TransactionCallback#afterTransaction(TransactionRequest, TransactionResponse, boolean)
     */
    @Override
    public void afterTransaction(
            final TransactionRequest request, TransactionResponse result, boolean committed) {
        // nothing to do
    }

    /**
     * @return {@code 0}, we don't need any special treatment
     * @see TransactionCallback#getPriority()
     */
    @Override
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }

    /**
     * Collects "transactionCustomizer.properties" file that contains a set of names and CQL
     * expressions, e.g.:
     *
     * <pre>
     *  UTENTE=env('GSUSER')
     *  AGGIORNAMENTO=now()
     *  </pre>
     *
     * <p>To keep things simple, the expressions will just use environment variables, but not see
     * the other values provided in the update/insert, and will not be differentiated by
     * insert/update cases.
     *
     * @see TransactionListener#dataStoreChange(TransactionEvent)
     */
    @Override
    public TransactionRequest beforeTransaction(TransactionRequest request) throws WFSException {
        if (request.getVersion() == null) {
            return request;
        }

        List<TransactionElement> newElements = new ArrayList<>();
        for (TransactionElement element : request.getElements()) {
            if (element instanceof Insert) {
                List<SimpleFeature> newFeatures = new ArrayList<>();
                for (Object of : affectedFeatures(element)) {
                    if (of instanceof SimpleFeature) {
                        try {
                            log.info("Updating feature: " + of);
                            SimpleFeature transformed = dataStoreChangeInternal((SimpleFeature) of);
                            newFeatures.add(transformed);
                        } catch (RuntimeException | IOException e) {
                            // Do never make the transaction fail due to an
                            // AutopopulateTransactionCallback error.
                            // Yell on the logs though
                            log.log(
                                    Level.WARNING,
                                    "Error pre computing the transaction's affected attributes",
                                    e);
                            newFeatures.add((SimpleFeature) of);
                        }
                    }
                }
                ((Insert) element).setFeatures(newFeatures);
                newElements.add(element);
            } else if (element instanceof Update) {
                FeatureTypeInfo featureTypeInfo =
                        getFeatureTypeInfo(new NameImpl(element.getTypeName()));
                try {
                    SimpleFeatureCollection features = getTransactionFeatures((Update) element);
                    SimpleFeatureIterator featuresIterator = features.features();
                    while (featuresIterator.hasNext()) {
                        SimpleFeature feature = featuresIterator.next();
                        log.info("Updating feature: " + feature);
                        SimpleFeature transformed = dataStoreChangeInternal(feature);
                        List<Property> properties = ((Update) element).getUpdateProperties();
                        for (org.geotools.api.feature.Property p : transformed.getProperties()) {
                            if (properties.stream()
                                    .anyMatch(
                                            prop ->
                                                    prop.getName()
                                                            .getLocalPart()
                                                            .equals(p.getName().getLocalPart()))) {
                                properties.stream()
                                        .filter(
                                                prop ->
                                                        prop.getName()
                                                                .getLocalPart()
                                                                .equals(p.getName().getLocalPart()))
                                        .forEach(prop -> prop.setValue(p.getValue()));
                            } else {
                                Property updateProperty = ((Update) element).createProperty();
                                updateProperty.setName(
                                        new QName(
                                                featureTypeInfo.getNamespace().getURI(),
                                                p.getName().getLocalPart()));
                                updateProperty.setValue(p.getValue());
                                properties.add(updateProperty);
                            }
                        }
                        ((Update) element).setUpdateProperties(properties);
                        transformed.getProperties().stream()
                                .forEach(p -> log.info("Feature Property: " + p));
                        ((Update) element)
                                .getUpdateProperties().stream()
                                        .forEach(
                                                p ->
                                                        log.info(
                                                                "Update Property: "
                                                                        + p.getName()
                                                                        + " "
                                                                        + p.getValue()));
                    }
                } catch (IOException e) {
                    // Do never make the transaction fail due to an
                    // AutopopulateTransactionCallback error.
                    // Yell on the logs though
                    log.log(
                            Level.WARNING,
                            "Error pre computing the transaction's affected attributes",
                            e);
                }
                newElements.add(element);
            } else if (element instanceof Delete) {
                newElements.add(element);
            }
        }

        // Replace the elements in the request
        request.setElements(newElements);
        return request;
    }

    /**
     * Get the feature type info for the given feature type.
     *
     * @param featureType
     * @return
     */
    private FeatureTypeInfo getFeatureTypeInfo(FeatureType featureType) {
        Name featureTypeName = featureType.getName();
        return getFeatureTypeInfo(featureTypeName);
    }

    /**
     * Get the feature type info for the given feature type name.
     *
     * @param featureTypeName
     * @return
     */
    private FeatureTypeInfo getFeatureTypeInfo(Name featureTypeName) {
        FeatureTypeInfo featureTypeInfo = catalog.getFeatureTypeByName(featureTypeName);
        if (featureTypeInfo == null) {
            throw new RuntimeException(
                    String.format("Couldn't find feature type info ''%s.", featureTypeName));
        }
        return featureTypeInfo;
    }

    /**
     * Get the features that are affected by the given update.
     *
     * @param update
     * @return
     */
    private SimpleFeatureCollection getTransactionFeatures(Update update) throws IOException {
        QName typeName = update.getTypeName();
        Filter filter = update.getFilter();
        SimpleFeatureSource source = getTransactionSource(update);
        try {
            List<SimpleFeature> recent = DataUtilities.list(source.getFeatures(filter));
            List<SimpleFeature> newFeatures =
                    recent.stream()
                            .map(f -> prepareUpdateFeature(f, update))
                            .collect(Collectors.toList());
            return DataUtilities.collection(newFeatures);
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Error getting features of type '%s'.", typeName), exception);
        }
    }

    /**
     * Set the updated attributes for the given feature.
     *
     * @param feature
     * @param update
     */
    private SimpleFeature prepareUpdateFeature(SimpleFeature feature, Update update) {
        // run the update
        for (Object o : update.getUpdateProperties()) {
            Property p = (Property) o;
            feature.setAttribute(p.getName().getLocalPart(), p.getValue());
        }
        return feature;
    }

    /**
     * Get the feature source for the given update.
     *
     * @param update
     * @return
     */
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

    /**
     * Get the list of affected features from the transaction element.
     *
     * @param element
     * @return
     */
    private List affectedFeatures(TransactionElement element) {
        if (element instanceof Insert) {
            return ((Insert) element).getFeatures();
        } else if (element instanceof Replace) {
            return ((Replace) element).getFeatures();
        }
        return new ArrayList<>();
    }

    /**
     * Check/alter feature collections and filters before a change hits the datastore.
     *
     * <p><b>Note</b> that caution should be exercised when relying on feature identifiers from a
     * {@link TransactionEventType#POST_INSERT} event. Depending on the type of store, those
     * identifiers may be reliable. Essentially, they can only be relied upon in the case of a
     * spatial dbms (such as PostGIS) is being used.
     *
     * @return
     */
    private SimpleFeature dataStoreChangeInternal(SimpleFeature source) throws IOException {
        log.info("Feature: " + source.getID());

        AutopopulateTemplate t =
                lookupTemplate(source.getFeatureType(), TRANSACTION_CUSTOMIZER_PROPERTIES);
        log.info("Template: " + t);

        if (t != null) {
            for (Map.Entry<String, String> entry : t.getAllProperties().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (source.getProperties().stream()
                        .anyMatch(p -> p.getName().getLocalPart().equals(key))) {
                    source.setAttribute(key, value);
                }
            }
        }

        return source;
    }

    /**
     * Returns the template for the specified feature type. Looking up templates is pretty
     * expensive, so we cache templates by feature type and template.
     */
    public AutopopulateTemplate lookupTemplate(SimpleFeatureType featureType, String path)
            throws IOException {

        // lookup the cache first
        TemplateKey key = new TemplateKey(featureType, path);
        AutopopulateTemplate t = templateCache.get(key);
        if (t != null) return t;

        // if not found, load the template
        GeoServerResourceLoader resourceLoader =
                GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        FeatureTypeInfo feature = catalog.getFeatureTypeByName(featureType.getTypeName());
        AutopopulateTemplateLoader templateLoader =
                new AutopopulateTemplateLoader(
                        resourceLoader, feature.getStore().getWorkspace(), feature);
        t = templateLoader.loadTemplate(path);
        templateCache.put(key, t);
        return t;
    }

    private static class TemplateKey {
        SimpleFeatureType type;
        String template;

        public TemplateKey(SimpleFeatureType type, String template) {
            super();
            this.type = type;
            this.template = template;
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((template == null) ? 0 : template.hashCode());
            result = PRIME * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final TemplateKey other = (TemplateKey) obj;
            if (template == null) {
                if (other.template != null) return false;
            } else if (!template.equals(other.template)) return false;
            if (type == null) {
                return other.type == null;
            } else return type.equals(other.type);
        }
    }
}
