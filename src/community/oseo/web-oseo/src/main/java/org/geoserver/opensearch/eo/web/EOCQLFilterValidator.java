/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.ProductClass;
import org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.resource.ResourceConfigurationException;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.text.ecql.ECQL;

/**
 * Validator for CQL filters used in EO OpenSearch configuration, validating both syntax and attribute names against the
 * OpenSearch schema.
 */
public class EOCQLFilterValidator implements IValidator<String> {

    private final boolean isProduct;

    public EOCQLFilterValidator(boolean isProduct) {
        this.isProduct = isProduct;
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        try {
            validateCqlFilter(validatable.getValue());
        } catch (Exception e) {
            ValidationError error = new ValidationError();
            error.setMessage("Invalid CQL filter: [" + validatable.getValue() + "]. " + e.getMessage());
            validatable.error(error);
        }
    }

    /*
     * Validate that CQL filter syntax is valid, and attribute names
     * used in the CQL filter are actually part of the OpenSearch schema (assuming there are no nested properties,
     * the complex features here are used only for list attributes).
     */
    private void validateCqlFilter(String cqlFilterString) throws Exception {
        if (cqlFilterString != null && !cqlFilterString.isEmpty()) {
            // step 1, validate CQL syntax
            Filter filter = ECQL.toFilter(cqlFilterString);

            // step 2, validate attribute names if possible
            GeoServerApplication app = GeoServerApplication.get();
            if (app == null) return;

            OpenSearchAccessProvider provider = app.getBeanOfType(OpenSearchAccessProvider.class);
            if (provider == null) return;

            OSEOInfo service = app.getGeoServer().getService(OSEOInfo.class);
            if (service == null) return;

            Map<String, String> namespaceSupport = getPrefixToNamespace(service);

            OpenSearchAccess openSearchAccess = provider.getOpenSearchAccess();
            FeatureType schema = isProduct
                    ? openSearchAccess.getProductSource().getSchema()
                    : openSearchAccess.getCollectionSource().getSchema();

            FilterAttributeExtractor filterAttributes = new FilterAttributeExtractor(null);
            filter.accept(filterAttributes, null);
            Set<String> filterAttributesNames = filterAttributes.getAttributeNameSet();
            for (String filterAttributeName : filterAttributesNames) {
                // split and validate the name, considering only the first part of the xpath (nested properties could be
                // in jsonb, dynamic)
                String[] parts = filterAttributeName.split("/");
                // then parse prefix and local name
                String[] splitted = parts[0].split(":");
                String namespace, localName;
                if (splitted.length == 1) {
                    namespace = null;
                    localName = splitted[0];
                } else if (splitted.length == 2) {
                    namespace = namespaceSupport.get(splitted[0]);
                    if (namespace == null) {
                        throw new ResourceConfigurationException(
                                ResourceConfigurationException.CQL_ATTRIBUTE_NAME_NOT_FOUND_$1,
                                new Object[] {filterAttributeName});
                    }
                    localName = splitted[1];
                } else {
                    throw new ResourceConfigurationException(
                            ResourceConfigurationException.CQL_ATTRIBUTE_NAME_NOT_FOUND_$1,
                            new Object[] {filterAttributeName});
                }

                if (!schema.getDescriptors().stream()
                        .map(d -> d.getName())
                        .anyMatch(n -> n.getLocalPart().equals(localName)
                                && (namespace == null || n.getNamespaceURI().equals(namespace)))) {
                    throw new ResourceConfigurationException(
                            ResourceConfigurationException.CQL_ATTRIBUTE_NAME_NOT_FOUND_$1,
                            new Object[] {filterAttributeName});
                }
            }
        }
    }

    private Map<String, String> getPrefixToNamespace(OSEOInfo service) {
        Map<String, String> result = new HashMap<>();
        result.put(JDBCOpenSearchAccess.EO_PREFIX, OpenSearchAccess.EO_NAMESPACE);
        result.put(JDBCOpenSearchAccess.EOP_PREFIX, OpenSearchAccess.EO_NAMESPACE);
        for (ProductClass productClass : service.getProductClasses()) {
            result.put(productClass.getPrefix(), productClass.getNamespace());
        }
        return result;
    }
}
