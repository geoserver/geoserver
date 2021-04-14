/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.validation.AbstractTemplateValidator;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.FeatureSource;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.xml.sax.helpers.NamespaceSupport;

/** Provides access to the product templates used for the OpenSearch GeoJSON outputs */
public class OpenSearchTemplates extends AbstractTemplates {

    static final Logger LOGGER = Logging.getLogger(OpenSearchTemplates.class);

    private Template productsTemplate;
    private Template collectionsTemplate;

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
        this.productsTemplate = new Template(items, new TemplateReaderConfiguration(namespaces));
    }

    /** Returns the products template */
    public RootBuilder getProductsTemplate() throws IOException {
        // load templates lazily, on startup the OpenSearchAccess might not yet be configured
        if (productsTemplate == null) reloadTemplates();

        RootBuilder builder = productsTemplate.getRootBuilder();
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
