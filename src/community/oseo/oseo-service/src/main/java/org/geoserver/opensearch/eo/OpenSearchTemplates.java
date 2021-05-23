/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.validation.AbstractTemplateValidator;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.FilterFactory2;
import org.xml.sax.helpers.NamespaceSupport;

/** Provides access to the product templates used for the OpenSearch GeoJSON outputs */
public class OpenSearchTemplates extends AbstractTemplates {

    static final Logger LOGGER = Logging.getLogger(OpenSearchTemplates.class);
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private Template collectionsTemplate;
    private Template defaultProductsTemplate;
    private ConcurrentHashMap<String, Template> productsTemplates = new ConcurrentHashMap<>();

    public OpenSearchTemplates(GeoServerDataDirectory dd, OpenSearchAccessProvider accessProvider)
            throws IOException {
        super(dd, accessProvider);
    }

    public void reloadTemplates() {
        try {
            OpenSearchAccess access = accessProvider.getOpenSearchAccess();
            try {
                reloadProductTemplate(dd, getNamespaces(access.getProductSource()));
            } catch (Exception e) {
                LOGGER.log(
                        Level.SEVERE, "Failed to load OpenSearch for EO JSON product templates", e);
            } finally {
                reloadCollectionTemplate(dd, getNamespaces(access.getCollectionSource()));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load OpenSearch for EO JSON templates", e);
        }
    }

    static NamespaceSupport getNamespaces(FeatureSource<FeatureType, Feature> fs) {
        // collect properties from all namespaces
        FeatureType schema = fs.getSchema();
        NamespaceSupport namespaces = new NamespaceSupport();
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            String uri = pd.getName().getNamespaceURI();
            String prefix = (String) pd.getUserData().get(OpenSearchAccess.PREFIX);
            if (prefix == null) throw new RuntimeException("No prefix available for " + uri);
            namespaces.declarePrefix(prefix, uri);
        }
        // done last, to avoid overrides
        namespaces.declarePrefix("", schema.getName().getNamespaceURI());
        return namespaces;
    }

    private void reloadProductTemplate(GeoServerDataDirectory dd, NamespaceSupport namespaces)
            throws IOException {
        // setup the items template
        Resource items = dd.get("templates/os-eo/products.json");
        copyDefault(items, "products.json");
        this.defaultProductsTemplate =
                new Template(items, new TemplateReaderConfiguration(namespaces));
    }

    /**
     * Returns the list of collection names having a custom template for them. First uses the
     *
     * @return
     */
    public Set<String> getCustomProductTemplates() throws IOException {
        Resource resource = dd.get("templates/os-eo/");
        if (resource.getType() != Resource.Type.DIRECTORY) return Collections.emptySet();

        Set<String> candidates =
                resource.list()
                        .stream()
                        .filter(r -> r.getType() == Resource.Type.RESOURCE)
                        .map(r -> r.name())
                        .filter(n -> n.startsWith("products-") && n.endsWith(".json"))
                        .map(n -> n.substring(6, n.length() - 5))
                        .collect(Collectors.toSet());

        // TODO: make this visit optimizable
        FeatureSource<FeatureType, Feature> collections =
                accessProvider.getOpenSearchAccess().getCollectionSource();
        UniqueVisitor visitor =
                new UniqueVisitor(FF.property("eo:identifier", getNamespaces(collections)));
        collections.getFeatures().accepts(visitor, null);
        candidates.retainAll(getCollectionNames(visitor));

        return candidates;
    }

    private Set<String> getCollectionNames(UniqueVisitor visitor) {
        @SuppressWarnings("unchecked")
        Set<Object> values = visitor.getResult().toSet();
        return values.stream()
                .map(o -> (String) ((o instanceof Attribute) ? ((Attribute) o).getValue() : o))
                .collect(Collectors.toSet());
    }

    /** Returns the products template */
    public RootBuilder getProductsTemplate(String collectionId) throws IOException {
        // load templates lazily, on startup the OpenSearchAccess might not yet be configured
        if (defaultProductsTemplate == null) reloadTemplates();

        Template template = defaultProductsTemplate;
        // See if a collection specific template has been setup, if so use it as an override
        if (collectionId != null) {
            Resource resource = dd.get("templates/os-eo/products-" + collectionId + ".json");
            if (resource.getType().equals(Resource.Type.RESOURCE)) {
                template = productsTemplates.get(collectionId);
                if (template == null) {
                    OpenSearchAccess access = accessProvider.getOpenSearchAccess();
                    template =
                            new Template(
                                    resource,
                                    new TemplateReaderConfiguration(
                                            getNamespaces(access.getProductSource())));
                    productsTemplates.put(collectionId, template);
                }
            }
        }

        template.checkTemplate();

        RootBuilder builder = template.getRootBuilder();
        if (builder != null)
            validate(
                    builder,
                    new AbstractTemplateValidator() {
                        @Override
                        protected FeatureType getFeatureType() throws IOException {
                            return accessProvider
                                    .getOpenSearchAccess()
                                    .getProductSource()
                                    .getSchema();
                        }
                    },
                    accessProvider.getOpenSearchAccess().getProductSource());

        return builder;
    }

    private void reloadCollectionTemplate(GeoServerDataDirectory dd, NamespaceSupport namespaces)
            throws IOException {
        // setup the collections template
        Resource items = dd.get("templates/os-eo/collections.json");
        copyDefault(items, "collections.json");
        this.collectionsTemplate = new Template(items, new TemplateReaderConfiguration(namespaces));
    }

    /** Returns the products template */
    public RootBuilder getCollectionsTemplate() throws IOException {
        // load templates lazily, on startup the OpenSearchAccess might not yet be configured
        if (collectionsTemplate == null) reloadTemplates();

        RootBuilder builder = collectionsTemplate.getRootBuilder();
        if (builder != null)
            validate(
                    builder,
                    new AbstractTemplateValidator() {
                        @Override
                        protected FeatureType getFeatureType() throws IOException {
                            return accessProvider
                                    .getOpenSearchAccess()
                                    .getCollectionSource()
                                    .getSchema();
                        }
                    },
                    accessProvider.getOpenSearchAccess().getCollectionSource());

        return builder;
    }
}
