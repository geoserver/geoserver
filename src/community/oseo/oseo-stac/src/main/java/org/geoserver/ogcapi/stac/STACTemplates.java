/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.eclipse.emf.common.util.URI;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.featurestemplating.builders.BuilderFactory;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.validation.AbstractTemplateValidator;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.springframework.stereotype.Component;
import org.xml.sax.helpers.NamespaceSupport;

/** Provides access to the collection and product templates used for the STAC JSON outputs */
@Component
public class STACTemplates {

    static final Logger LOGGER = Logging.getLogger(STACTemplates.class);

    private final OpenSearchAccessProvider accessProvider;
    private final GeoServerDataDirectory dd;
    private Template collectionTemplate;
    private Template productTemplate;

    public STACTemplates(GeoServerDataDirectory dd, OpenSearchAccessProvider accessProvider)
            throws IOException {
        this.accessProvider = accessProvider;
        this.dd = dd;
    }

    public void reloadTemplates() {
        try {
            Name name =
                    accessProvider
                            .getOpenSearchAccess()
                            .getCollectionSource()
                            .getSchema()
                            .getName();
            NamespaceSupport namespaces = new NamespaceSupport();
            namespaces.declarePrefix("", name.getNamespaceURI());

            try {
                reloadCollectionTemplate(dd, namespaces);
            } finally {
                reloadProductTemplate(dd, namespaces);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load STAC JSON templates", e);
        }
    }

    private void reloadProductTemplate(GeoServerDataDirectory dd, NamespaceSupport namespaces)
            throws IOException {
        // setup the products template
        Resource products = dd.get("templates/ogc/stac/products.json");
        copyDefault(products, "products.json");
        this.productTemplate = new Template(products, new TemplateReaderConfiguration(namespaces));
    }

    private void reloadCollectionTemplate(GeoServerDataDirectory dd, NamespaceSupport namespaces)
            throws IOException {
        // setup the collections template
        Resource collections = dd.get("templates/ogc/stac/collections.json");
        copyDefault(collections, "collections.json");
        TemplateReaderConfiguration configuration =
                new TemplateReaderConfiguration(namespaces) {
                    @Override
                    // the collections are not a GeoJSON collection, uses a different structure
                    public BuilderFactory getBuilderFactory(boolean isJSONLD) {
                        return new BuilderFactory(isJSONLD, "collections");
                    }
                };
        this.collectionTemplate = new Template(collections, configuration);
    }

    private void copyDefault(Resource collections, String defaultTemplate) throws IOException {
        if (collections.getType() == Resource.Type.UNDEFINED) {
            try (InputStream is = STACTemplates.class.getResourceAsStream(defaultTemplate);
                    OutputStream os = collections.out()) {
                IOUtils.copy(is, os);
            }
        }
    }

    /** Returns the collections template */
    public RootBuilder getCollectionTemplate() throws IOException {
        // load templates lazily, on startup the OpenSearchAccess might not yet be configured
        if (collectionTemplate == null) reloadTemplates();

        RootBuilder builder = collectionTemplate.getRootBuilder();
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
                    });

        return builder;
    }

    /** Returns the product template */
    public RootBuilder getProductTemplate() throws IOException {
        // load templates lazily, on startup the OpenSearchAccess might not yet be configured
        if (productTemplate == null) reloadTemplates();

        RootBuilder builder = productTemplate.getRootBuilder();
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
                    });

        return builder;
    }

    private void validate(RootBuilder root, AbstractTemplateValidator validator)
            throws IOException {
        if (root != null) {
            boolean isValid = validator.validateTemplate(root);
            if (!isValid) {
                throw new RuntimeException(
                        "Failed to validate template for "
                                + validator.getTypeName()
                                + ". Failing attribute is "
                                + URI.decode(validator.getFailingAttribute()));
            }
        }
    }
}
