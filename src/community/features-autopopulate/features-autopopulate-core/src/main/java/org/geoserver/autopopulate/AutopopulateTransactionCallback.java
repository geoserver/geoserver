/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.autopopulate;

import static org.geotools.data.DataUtilities.first;
import static org.geotools.data.DataUtilities.simple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wfs.TransactionCallback;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.WFSTransactionException;
import org.geoserver.wfs.request.Delete;
import org.geoserver.wfs.request.Insert;
import org.geoserver.wfs.request.Property;
import org.geoserver.wfs.request.Replace;
import org.geoserver.wfs.request.TransactionElement;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geoserver.wfs.request.Update;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;

/**
 * Listens to transactions (so far only issued by WFS), and autopopulates the feature type attributes according to the
 * values retrieved from the properties file.
 *
 * <p>A Spring bean singleton of this class needs to be declared in order for GeoServer transactions to pick it up
 * automatically and forward transaction events to it.
 *
 * <p>The plugin uses a custom TransactionCallback that alters the insert/update WFS-T operations, forcing in specific
 * values into them, based on configuration files.
 *
 * <p>To support configuration for multiple layers, the easiest thing is to place a configuration, file in the
 * directories of the layers themselves, pretty much like the featureinfo templates.
 *
 * <p>A "transactionCustomizer.properties" file that contains a set of names and CQL expressions e.g.:
 *
 * <pre>
 * UTENTE=env('GSUSER') # this will be replaced with the current user see @EnviromentInjectionCallback
 * AGGIORNAMENTO=now()  # this will be replaced with the current date
 * </pre>
 *
 * <p>To keep things simple, the expressions will just use environment variables, but not see the other values provided
 * in the update/insert, and will not be differentiated by insert/update cases.
 *
 * @author Alessio Fabiani, GeoSolutions SRL, alessio.fabiani@geosolutionsgroup.com
 * @see TransactionCallback
 */
public class AutopopulateTransactionCallback implements TransactionCallback {

    public static final String TRANSACTION_CUSTOMIZER_PROPERTIES = "transactionCustomizer.properties";
    /** logger */
    private static final Logger LOGGER = Logging.getLogger(AutopopulateTransactionCallback.class);
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
        LOGGER.info("AutopopulateTransactionCallback initialized");
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
    public void afterTransaction(final TransactionRequest request, TransactionResponse result, boolean committed) {
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
     * Collects "transactionCustomizer.properties" file that contains a set of names and CQL expressions, e.g.:
     *
     * <pre>
     *  UTENTE=env('GSUSER')
     *  AGGIORNAMENTO=now()
     *  </pre>
     *
     * <p>To keep things simple, the expressions will just use environment variables, but not see the other values
     * provided in the update/insert, and will not be differentiated by insert/update cases.
     *
     * @see TransactionCallback#beforeTransaction(TransactionRequest)
     */
    @Override
    public TransactionRequest beforeTransaction(TransactionRequest request) throws WFSException {
        if (request.getVersion() == null) {
            return request;
        }

        List<TransactionElement> newElements = new ArrayList<>();
        for (TransactionElement element : request.getElements()) {
            if (element instanceof Insert insertElement) {
                List<SimpleFeature> newFeatures = new ArrayList<>();
                for (Object of : affectedFeatures(element)) {
                    if (of instanceof SimpleFeature feature) {
                        try {
                            LOGGER.fine("Inserting feature: " + of);
                            SimpleFeature transformed = applyTemplate(feature);
                            LOGGER.fine("... transformed: " + transformed);
                            newFeatures.add(transformed);
                        } catch (RuntimeException | IOException e) {
                            // Do never make the transaction fail due to an
                            // AutopopulateTransactionCallback error.
                            // Yell on the logs though
                            LOGGER.log(Level.WARNING, "Error pre computing the transaction's affected attributes", e);
                            newFeatures.add(feature);
                        }
                    }
                }
                insertElement.setFeatures(newFeatures);
                newElements.add(element);
            } else if (element instanceof Update updateElement) {
                FeatureTypeInfo featureTypeInfo = getFeatureTypeInfo(new NameImpl(element.getTypeName()));
                try {
                    SimpleFeature feature = getTransactionFeatureTemplate(updateElement);
                    LOGGER.fine("Updating feature: " + feature);
                    SimpleFeature transformed = applyTemplate(feature);
                    LOGGER.fine("... transformed: " + transformed);
                    List<Property> properties = updateElement.getUpdateProperties();
                    for (org.geotools.api.feature.Property p : transformed.getProperties()) {
                        if (properties.stream().anyMatch(prop -> match(p, prop))) {
                            properties.stream()
                                    .filter(prop -> match(p, prop))
                                    .forEach(prop -> prop.setValue(prop.getValue()));
                        } else {
                            Property updateProperty = updateElement.createProperty();
                            updateProperty.setName(new QName(
                                    featureTypeInfo.getNamespace().getURI(),
                                    p.getName().getLocalPart()));
                            updateProperty.setValue(p.getValue());
                            properties.add(updateProperty);
                        }
                    }
                    updateElement.setUpdateProperties(properties);
                } catch (IOException e) {
                    // Do never make the transaction fail due to an
                    // AutopopulateTransactionCallback error.
                    // Yell on the logs though
                    LOGGER.log(Level.WARNING, "Error pre computing the transaction's affected attributes", e);
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

    /** Utility method to match a feature property with a property. */
    private static boolean match(org.geotools.api.feature.Property p, Property prop) {
        return prop.getName().getLocalPart().equals(p.getName().getLocalPart());
    }

    /**
     * Get the feature type info for the given feature type name.
     *
     * @param featureTypeName the feature type name
     * @return FeatureTypeInfo the feature type info
     */
    private FeatureTypeInfo getFeatureTypeInfo(Name featureTypeName) {
        FeatureTypeInfo featureTypeInfo = catalog.getFeatureTypeByName(featureTypeName);
        if (featureTypeInfo == null) {
            throw new RuntimeException("Couldn't find feature type info ''%s.".formatted(featureTypeName));
        }
        return featureTypeInfo;
    }

    /**
     * Get the feature template for the given update.
     *
     * @param update the update transaction element
     * @return SimpleFeature the feature template with the updated properties
     */
    @SuppressWarnings("unchecked")
    private SimpleFeature getTransactionFeatureTemplate(Update update) throws IOException {
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
        return first(simple(source.getFeatures(update.getFilter())));
    }

    /**
     * Get the list of affected features from the transaction element.
     *
     * @param element the transaction element
     * @return List of affected features
     */
    private List affectedFeatures(TransactionElement element) {
        if (element instanceof Insert insert) {
            return insert.getFeatures();
        } else if (element instanceof Replace replace) {
            return replace.getFeatures();
        }
        return new ArrayList<>();
    }

    /**
     * Check/alter feature collections and filters before a change hits the datastore.
     *
     * <p><b>Note</b> that caution should be exercised when relying on feature identifiers from a
     * {@link TransactionEventType#POST_INSERT} event. Depending on the type of store, those identifiers may be
     * reliable. Essentially, they can only be relied upon in the case of a spatial dbms (such as PostGIS) is being
     * used.
     *
     * @return SimpleFeature the feature to be persisted
     */
    private SimpleFeature applyTemplate(SimpleFeature source) throws IOException {
        AutopopulateTemplate t = lookupTemplate(source.getFeatureType(), TRANSACTION_CUSTOMIZER_PROPERTIES);
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
     * Returns the template for the specified feature type. Looking up templates is pretty expensive, so we cache
     * templates by feature type and template.
     */
    public AutopopulateTemplate lookupTemplate(SimpleFeatureType featureType, String path) throws IOException {

        // lookup the cache first
        TemplateKey key = new TemplateKey(featureType, path);
        AutopopulateTemplate t = templateCache.get(key);
        if (t != null && !t.needsReload()) return t;

        // if not found, load the template
        GeoServerResourceLoader resourceLoader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        FeatureTypeInfo feature = catalog.getFeatureTypeByName(featureType.getTypeName());
        AutopopulateTemplateLoader templateLoader = new AutopopulateTemplateLoader(resourceLoader, feature);
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof TemplateKey)) return false;
            TemplateKey that = (TemplateKey) o;
            return Objects.equals(type, that.type) && Objects.equals(template, that.template);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, template);
        }
    }
}
