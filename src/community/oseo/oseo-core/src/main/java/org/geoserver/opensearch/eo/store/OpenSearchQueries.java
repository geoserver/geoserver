/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.factory.CommonFactoryFinder;

/** Helper class factoring out some common code */
public class OpenSearchQueries {

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    /**
     * Returns all the simple properties of products, plus the collection, so that it can be used in
     * the templates. For the time being, skips the OGC links thumbnails and the like.
     */
    public static List<PropertyName> getProductProperties(OpenSearchAccess osa) throws IOException {
        Collection<PropertyDescriptor> descriptors =
                osa.getProductSource().getSchema().getDescriptors();
        return descriptors.stream()
                .filter(pd -> isSimple(pd) || isCollection(pd))
                .map(pd -> FF.property(pd.getName()))
                .collect(Collectors.toList());
    }

    private static boolean isCollection(PropertyDescriptor pd) {
        return JDBCOpenSearchAccess.COLLECTION_PROPERTY_NAME.equals(pd.getName());
    }

    private static boolean isSimple(PropertyDescriptor pd) {
        Class<?> binding = pd.getType().getBinding();
        return !(List.class.isAssignableFrom(binding))
                && !(FeatureType.class.isAssignableFrom(binding));
    }
}
