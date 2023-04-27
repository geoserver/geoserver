/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.configuration.Template;
import org.geoserver.featurestemplating.readers.TemplateReaderConfiguration;
import org.geoserver.featurestemplating.validation.AbstractTemplateValidator;
import org.geoserver.opensearch.eo.AbstractTemplates;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.xml.sax.helpers.NamespaceSupport;

/** Provides access to the collection and item templates used for the STAC JSON outputs */
@Component
public class STACTemplates extends AbstractTemplates {

    static final Logger LOGGER = Logging.getLogger(STACTemplates.class);
    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    private static final String COLLECTIONS = "collections";

    private Template defaultCollectionTemplate;
    private Template defaultItemTemplate;
    private ConcurrentHashMap<String, Template> itemTemplates = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Template> collectionTemplates = new ConcurrentHashMap<>();

    ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    public STACTemplates(GeoServerDataDirectory dd, OpenSearchAccessProvider accessProvider)
            throws IOException {
        super(dd, accessProvider);
    }

    /** Copies over all HTML templates, to allow customization */
    protected void copyHTMLTemplates() throws IOException {
        Resource stac = dd.get("templates/ogc/stac/v1");
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
        Resource items = dd.get("templates/ogc/stac/v1/items.json");
        copyDefault(items, "items.json");
        this.defaultItemTemplate = new Template(items, new TemplateReaderConfiguration(namespaces));
    }

    private void reloadCollectionTemplate(GeoServerDataDirectory dd, NamespaceSupport namespaces)
            throws IOException {
        // setup the collections template
        Resource collections = dd.get("templates/ogc/stac/v1/collections.json");
        copyDefault(collections, "collections.json");
        TemplateReaderConfiguration configuration =
                new TemplateReaderConfiguration(namespaces, COLLECTIONS);
        this.defaultCollectionTemplate = new Template(collections, configuration);
    }

    /** Returns the collections template */
    public RootBuilder getCollectionTemplate(String collectionId) throws IOException {
        // load templates lazily, on startup the OpenSearchAccess might not yet be configured
        if (defaultCollectionTemplate == null) reloadTemplates();

        Template template = defaultCollectionTemplate;
        // See if a collection specific template has been setup, if so use it as an override
        if (collectionId != null) {
            Resource resource =
                    dd.get("templates/ogc/stac/v1/collections-" + collectionId + ".json");
            if (resource.getType().equals(Resource.Type.RESOURCE)) {
                template = collectionTemplates.get(collectionId);
                if (template == null) {
                    OpenSearchAccess access = accessProvider.getOpenSearchAccess();
                    TemplateReaderConfiguration configuration =
                            new TemplateReaderConfiguration(
                                    getNamespaces(access.getProductSource()), COLLECTIONS);
                    template = new Template(resource, configuration);
                    collectionTemplates.put(collectionId, template);
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

    /**
     * Returns the list of collection names having a custom template for them.
     *
     * @return
     */
    public Set<String> getCustomItemTemplates() throws IOException {
        Resource resource = dd.get("templates/ogc/stac/v1/");
        if (resource.getType() != Resource.Type.DIRECTORY) return Collections.emptySet();

        Set<String> candidates =
                resource.list().stream()
                        .filter(r -> r.getType() == Resource.Type.RESOURCE)
                        .map(r -> r.name())
                        .filter(n -> n.startsWith("items-") && n.endsWith(".json"))
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

    /**
     * Returns the item template for the specified collection
     *
     * @param collectionId The collection identifier, or null if no specific collection is required
     */
    public RootBuilder getItemTemplate(String collectionId) throws IOException {
        // load templates lazily, on startup the OpenSearchAccess might not yet be configured
        if (defaultItemTemplate == null) reloadTemplates();

        Template template = defaultItemTemplate;
        // See if a collection specific template has been setup, if so use it as an override
        if (collectionId != null) {
            Resource resource = dd.get("templates/ogc/stac/v1/items-" + collectionId + ".json");
            if (resource.getType().equals(Resource.Type.RESOURCE)) {
                template = itemTemplates.get(collectionId);
                if (template == null) {
                    OpenSearchAccess access = accessProvider.getOpenSearchAccess();
                    template =
                            new Template(
                                    resource,
                                    new TemplateReaderConfiguration(
                                            getNamespaces(access.getProductSource())));
                    itemTemplates.put(collectionId, template);
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
}
