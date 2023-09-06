/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.validation.AbstractTemplateValidator;
import org.geoserver.opensearch.eo.response.AtomSearchResponse;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.resource.Resource;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.xml.sax.helpers.NamespaceSupport;

/** Provides access to the product templates used for the OpenSearch GeoJSON outputs */
public class OpenSearchTemplates extends AbstractTemplates {

    static final Logger LOGGER = Logging.getLogger(OpenSearchTemplates.class);
    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    private Template defaultCollectionsTemplate;
    private Template defaultProductsTemplate;
    private ConcurrentHashMap<String, Template> productsTemplates = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Template> collectionsTemplates = new ConcurrentHashMap<>();

    ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public OpenSearchTemplates(GeoServerDataDirectory dd, OpenSearchAccessProvider accessProvider)
            throws IOException {
        super(dd, accessProvider);
    }

    /** Copies over all HTML templates, to allow customization */
    private void copyHTMLTemplates() throws IOException {
        Resource oseo = dd.get("templates/os-eo");
        oseo.dir();
        // HTML templates
        copyResources(
                oseo,
                "classpath:/"
                        + AtomSearchResponse.class.getPackage().getName().replace(".", "/")
                        + "/*.ftl");
        // metadata templates
        copyResources(
                oseo,
                "classpath:/"
                        + DefaultOpenSearchEoService.class.getPackage().getName().replace(".", "/")
                        + "/*.ftl");
    }

    private void copyResources(Resource oseo, String path) throws IOException {
        for (org.springframework.core.io.Resource r : resourceResolver.getResources(path)) {
            Resource target = oseo.get(r.getFilename());
            if (target.getType() == Resource.Type.UNDEFINED) {
                try (InputStream is = r.getInputStream();
                        OutputStream os = target.out()) {
                    IOUtils.copy(is, os);
                }
            }
        }
    }

    public void reloadTemplates() {
        try {
            copyHTMLTemplates();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to copy OpenSearch HTML templates", e);
        }

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
        this.defaultCollectionsTemplate =
                new Template(items, new TemplateReaderConfiguration(namespaces));
    }

    /** Returns the collections template */
    public RootBuilder getCollectionsTemplate(String collectionId) throws IOException {
        // load templates lazily, on startup the OpenSearchAccess might not yet be configured
        if (defaultCollectionsTemplate == null) reloadTemplates();

        Template template = defaultCollectionsTemplate;
        // See if a collection specific template has been setup, if so use it as an override
        if (collectionId != null) {
            Resource resource = dd.get("templates/os-eo/collections-" + collectionId + ".json");
            if (resource.getType().equals(Resource.Type.RESOURCE)) {
                template = collectionsTemplates.get(collectionId);
                if (template == null) {
                    OpenSearchAccess access = accessProvider.getOpenSearchAccess();
                    template =
                            new Template(
                                    resource,
                                    new TemplateReaderConfiguration(
                                            getNamespaces(access.getProductSource())));
                    collectionsTemplates.put(collectionId, template);
                }
            }
        }

        RootBuilder builder = template.getRootBuilder();
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
