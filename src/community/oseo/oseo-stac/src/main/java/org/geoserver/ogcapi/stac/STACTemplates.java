/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.featurestemplating.builders.BuilderFactory;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.validation.AbstractTemplateValidator;
import org.geoserver.opensearch.eo.AbstractTemplates;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.FeatureSource;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.xml.sax.helpers.NamespaceSupport;

/** Provides access to the collection and item templates used for the STAC JSON outputs */
@Component
public class STACTemplates extends AbstractTemplates {

    static final Logger LOGGER = Logging.getLogger(STACTemplates.class);

    private Template collectionTemplate;
    private Template itemTemplate;

    ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public STACTemplates(GeoServerDataDirectory dd, OpenSearchAccessProvider accessProvider)
            throws IOException {
        super(dd, accessProvider);
    }

    /** Copies over all HTML templates, to allow customization */
    private void copyHTMLTemplates() throws IOException {
        Resource stac = dd.get("templates/ogc/stac");
        stac.dir();
        String path =
                "classpath:/" + getClass().getPackage().getName().replace(".", "/") + "/*.ftl";
        for (org.springframework.core.io.Resource r : resourceResolver.getResources(path)) {
            Resource target = stac.get(r.getFilename());
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
            LOGGER.log(Level.SEVERE, "Failed to copy STAC HTML templates", e);
        }

        try {
            OpenSearchAccess access = accessProvider.getOpenSearchAccess();
            try {
                reloadCollectionTemplate(dd, getNamespaces(access.getCollectionSource()));
            } finally {
                reloadItemTemplate(dd, getNamespaces(access.getProductSource()));
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load STAC JSON templates", e);
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

    private void reloadItemTemplate(GeoServerDataDirectory dd, NamespaceSupport namespaces)
            throws IOException {
        // setup the items template
        Resource items = dd.get("templates/ogc/stac/items.json");
        copyDefault(items, "items.json");
        this.itemTemplate = new Template(items, new TemplateReaderConfiguration(namespaces));
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
                        public FeatureType getFeatureType() throws IOException {
                            return accessProvider
                                    .getOpenSearchAccess()
                                    .getCollectionSource()
                                    .getSchema();
                        }
                    },
                    accessProvider.getOpenSearchAccess().getCollectionSource());

        return builder;
    }

    /** Returns the item template */
    public RootBuilder getItemTemplate() throws IOException {
        // load templates lazily, on startup the OpenSearchAccess might not yet be configured
        if (itemTemplate == null) reloadTemplates();

        RootBuilder builder = itemTemplate.getRootBuilder();
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
}
