/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.io.IOException;
import java.util.logging.Logger;

import org.geotools.data.Join;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

/**
 * Maps joined simple features up to a complex Collection feature
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCProductFeatureSource extends AbstractMappingSource {

    static final Logger LOGGER = Logging.getLogger(JDBCProductFeatureSource.class);

    public JDBCProductFeatureSource(JDBCOpenSearchAccess openSearchAccess,
            FeatureType collectionFeatureType) throws IOException {
        super(openSearchAccess, collectionFeatureType);
    }

    protected SimpleFeatureSource getDelegateCollectionSource() throws IOException {
        return openSearchAccess.getDelegateStore().getFeatureSource(JDBCOpenSearchAccess.PRODUCT);
    }

    @Override
    protected String getMetadataTable() {
        return "product_metadata";
    }

    @Override
    protected Query mapToSimpleCollectionQuery(Query query) throws IOException {
        Query result = super.mapToSimpleCollectionQuery(query);

        // join to quicklook table if necessary
        if (hasOutputProperty(query, OpenSearchAccess.QUICKLOOK_PROPERTY_NAME)) {
            Filter filter = FF.equal(FF.property("id"), FF.property("quicklook.id"), true);
            Join join = new Join("product_thumb", filter);
            join.setAlias("quicklook");
            result.getJoins().add(join);
        }

        return result;

    }

    @Override
    protected void mapProperties(ComplexFeatureBuilder builder, SimpleFeature fi) {
        // basic mappings
        super.mapProperties(builder, fi);

        // quicklook extraction
        Object metadataValue = fi.getAttribute("quicklook");
        if (metadataValue instanceof SimpleFeature) {
            SimpleFeature quicklookFeature = (SimpleFeature) metadataValue;
            AttributeBuilder ab = new AttributeBuilder(CommonFactoryFinder.getFeatureFactory(null));
            ab.setDescriptor((AttributeDescriptor) schema
                    .getDescriptor(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME));
            Attribute attribute = ab.buildSimple(null, quicklookFeature.getAttribute("thumb"));
            builder.append(OpenSearchAccess.QUICKLOOK_PROPERTY_NAME, attribute);
        }
    }

}
