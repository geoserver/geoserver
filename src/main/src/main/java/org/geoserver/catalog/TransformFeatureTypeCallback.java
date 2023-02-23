/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.GeoServerDefaultLocale;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.transform.Definition;
import org.geotools.data.transform.TransformFactory;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.util.SimpleInternationalString;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.expression.Expression;
import org.opengis.util.InternationalString;

/**
 * Transforms a vector layer {@link org.opengis.feature.type.FeatureType} based on the definitions
 * contains in the eventual {@link AttributeTypeInfo} list.
 */
public class TransformFeatureTypeCallback {

    public FeatureType retypeFeatureType(FeatureTypeInfo fti, FeatureType schema)
            throws IOException {
        List<AttributeTypeInfo> attributes = fti.getAttributes();
        if (attributes == null || attributes.isEmpty()) return schema;

        // TODO: are we respecting the projection handling here??

        // apply the definitions and return the transformed feature type
        SimpleFeatureSource fs = getDataStore(fti).getFeatureSource(fti.getNativeName());
        SimpleFeatureSource tfs = getTransformedSource(fti, attributes, fs);
        return tfs.getSchema();
    }

    @SuppressWarnings("unchecked")
    public <T extends FeatureType, U extends Feature> FeatureSource<T, U> wrapFeatureSource(
            FeatureTypeInfo fti, FeatureSource<T, U> fs) throws IOException {
        List<AttributeTypeInfo> attributes = fti.getAttributes();
        if (attributes == null || attributes.isEmpty()) return fs;

        return (FeatureSource<T, U>)
                getTransformedSource(fti, attributes, (SimpleFeatureSource) fs);
    }

    private SimpleFeatureSource getTransformedSource(
            FeatureTypeInfo fti, List<AttributeTypeInfo> attributes, SimpleFeatureSource fs)
            throws IOException {
        List<Definition> definitions =
                attributes.stream().map(ati -> toDefinition(ati)).collect(Collectors.toList());
        SimpleFeatureSource tfs = TransformFactory.transform(fs, fti.getName(), definitions);
        return tfs;
    }

    private DataStore getDataStore(FeatureTypeInfo fti) throws IOException {
        DataAccess da = fti.getStore().getDataStore(null);
        if (da instanceof DataStore) return (DataStore) da;

        // should not have gotten here, validation should have prevented it
        throw new ServiceException(
                "Cannot apply feature type customization on complex features, the server is mis-configured");
    }

    private Definition toDefinition(AttributeTypeInfo ati) {
        try {
            String name = ati.getName();
            Expression source = ECQL.toExpression(ati.getSource());
            Class<?> binding = ati.getBinding();
            InternationalString descriptionInternational = ati.getDescription();
            String description = null;
            if (descriptionInternational != null) {
                description =
                        StringUtils.trimToNull(
                                ati.getDescription().toString(GeoServerDefaultLocale.get()));
                if (description != null) {
                    descriptionInternational = new SimpleInternationalString(description);
                } else {
                    descriptionInternational = null;
                }
            }
            return new Definition(name, source, binding, null, descriptionInternational);
        } catch (CQLException e) {
            throw new ServiceException(
                    "Failed to parse the attribute source definition to a valid OGC Expression", e);
        }
    }
}
