/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.common.util.URI;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.featurestemplating.builders.impl.RootBuilder;
import org.geoserver.featurestemplating.validation.AbstractTemplateValidator;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.platform.resource.Resource;
import org.geotools.data.FeatureSource;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.xml.sax.helpers.NamespaceSupport;

/** Base class providing support for template handling in OpenSearch for EO and STAC */
public abstract class AbstractTemplates {

    protected final OpenSearchAccessProvider accessProvider;
    protected final GeoServerDataDirectory dd;

    public AbstractTemplates(GeoServerDataDirectory dd, OpenSearchAccessProvider accessProvider) {
        this.accessProvider = accessProvider;
        this.dd = dd;
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

    /**
     * Helper to copy a given default template, from classpath to the data directory
     *
     * @param target
     * @param defaultTemplate
     * @throws IOException
     */
    protected void copyDefault(Resource target, String defaultTemplate) throws IOException {
        if (target.getType() == Resource.Type.UNDEFINED) {
            try (InputStream is = getClass().getResourceAsStream(defaultTemplate);
                    OutputStream os = target.out()) {
                IOUtils.copy(is, os);
            }
        }
    }

    /**
     * Valides the given {@link RootBuilder} against the structure of the feature source
     *
     * @param root
     * @param validator
     * @param source
     * @throws IOException
     */
    protected void validate(
            RootBuilder root,
            AbstractTemplateValidator validator,
            FeatureSource<FeatureType, Feature> source)
            throws IOException {
        if (root != null) {
            boolean isValid = validator.validateTemplate(root);
            if (!isValid) {
                throw new RuntimeException(
                        "Failed to validate template for "
                                + validator.getTypeName()
                                + ". Failing attribute is "
                                + URI.decode(validator.getFailingAttribute())
                                + "\n"
                                + availableAttributesSuffix(
                                        source.getSchema(), getNamespaces(source)));
            }
        }
    }

    protected String availableAttributesSuffix(Object ctx, NamespaceSupport ns) {
        if (ctx instanceof FeatureType) {
            FeatureType ft = (FeatureType) ctx;
            String values =
                    ft.getDescriptors()
                            .stream()
                            .map(ad -> attributeName(ad, ns))
                            .collect(Collectors.joining(", "));
            if (!StringUtils.isEmpty(values)) return " Available attributes: " + values;
        }
        return "";
    }

    protected String attributeName(PropertyDescriptor ad, NamespaceSupport ns) {
        String name = ad.getName().getLocalPart();
        String uri = ad.getName().getNamespaceURI();
        if (ns != null && uri != null && !StringUtils.isEmpty(ns.getPrefix(uri))) {
            name = ns.getPrefix(uri) + ":" + name;
        }

        return name;
    }

    /**
     * Reloads the templates. Called by {@link TemplatesReloader} on big configuration event changes
     * (reset, reload, possibly more).
     */
    public abstract void reloadTemplates();
}
